import time
import sys
import os
import datetime
import json
from datetime import timedelta
from threading import Thread, current_thread
import RPi.GPIO as GPIO
import base64

'''
    BEGIN CONFIGURATIE FILE
'''
pir_pin = 4
output_pin1 = 18
output_pin2 = 17
pin_high_time = 5
write_log_to_db = True
audio_p = 'mpg123' 

sound_dir = '/home/pi/Desktop/Sounds/'
host_dir = '/home/pi/Documents/Domotica/'

sql_server = 'nmct-domotica.database.windows.net'
sql_user = 'student@nmct-domotica'
sql_passw = '-Azerty2016'
sql_database = 'SecuritySystem'

connection_string = 'HostName=nmct-domotica.azure-devices.net;DeviceId=SanderPi;SharedAccessKey=CzPa/njonBfQNbGBkttE95wxVejqga9IBKco7i9PqO8='

cam_res_width = 640
cam_res_height = 480
cam_framerate = 30

'''
    BEGIN CONFIGURATIE FILE
'''

def update_log(message,type,db):
    #Update logboek database
    timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')
    print str(timestamp)+': '+message
    if write_log_to_db == True and db == True:
        write_database_log(message,type)
    time.sleep(3)

def update_status(status):
    #Update security status
    if status != security_status['current_status']:
        security_status['previous_status'] = security_status['current_status']
        security_status['current_status'] = status
        security_status['last_status_change'] = time.time()
        update_log(status,'status update',False)

def acces_denied():
    audio_queue.append('Acces_denied.mp3')
    update_log('acces denied','event',True)

def unlock_door(email):
    audio_queue.append('Acces_granted.mp3')
    update_log('door unlocked by user: '+email,'event',True)
    #UNLOCK DOOR OR DO SOMETHING ELSE
    GPIO.output(output_pin1, GPIO.HIGH)
    GPIO.output(output_pin2, GPIO.HIGH)
    time.sleep(pin_high_time)
    GPIO.output(output_pin1, GPIO.LOW)
    GPIO.output(output_pin2, GPIO.LOW)     

def check_if_valid(code):
    for char in code:
        if char.isdigit() == False:
            return False
    return True

def remove_database_code(code):
    try:
        global cursor
        cursor.execute('DELETE FROM LoginCodes WHERE Code= %s',(code))
    except Exception as e:
        print e
        update_log('unexpected error from sql database','error',True)

def read_database_code(code):
    
    if check_if_valid(code) == False:
        return None
    try:
        global cursor       
        cursor.execute('SELECT * FROM LoginCodes WHERE Code= %s',(code))        
        for row in cursor:             
             return row
    except Exception as e:
        print e
        update_log('unexpected error from sql database','error',True)
        return None        

def write_database_log(logentry,logtype):
    try:
        global cursor, sqlcon
        cursor.execute("""
            INSERT INTO LogItems (Log,Type,Deleted)
            VALUES (%s,%s,%s) 
        """,(logentry,logtype,'False'))
        sqlcon.commit()
    except Exception as e:
        print e
        update_log('unexpected error from sql database','error',True)

#DAEMON THREAD
def qrcode_monitor():
    #Thread die qrcodes zoekt en vergelijkt
    time.sleep(1)
    print 'starting qrcode monitor'
    time.sleep(10)
    def decode_qrcode(img):
        image_string = img.tostring()
        try:
            code = zbarlight.qr_code_scanner(image_string,cam_res_width,cam_res_height)
            return code
        except:
            return    

    while True:
        if len (qrcode_queue) > 0:
            for image in list(qrcode_queue):
                decoded = decode_qrcode(image)
                if decoded is not None:                    
                    row = read_database_code(decoded)
                    if row is not None:
                        if str(row['Code']) == str(decoded):
                            update_log('qrcode detected','status update',False)
                            unlock_door(row['TargetEmail'])                            
                            #remove_database_code(row['Code'])
                    elif row is None:
                        acces_denied()
                qrcode_queue.remove(image)
        time.sleep(1)

#DAEMON THREAD
def photo_processor(): 
    #Thread die de photo's zal bekijken en uploaden
    time.sleep(2)
    print 'starting photo processor'
    time.sleep(10)
    while True:
        if len(photo_queue) > 0:
            for photo in list(photo_queue):
                                
                filename = os.path.basename(photo)
                filename = filename.replace(":", "")
                filename = filename.replace("-", "")
                filename = filename.replace(" ", "")
                
                upload_to_blob(filename,photo)
                photo_queue.remove(photo)
        time.sleep(1)

#DAEMON THREAD
def audio_player():
    #Audio playlist
    time.sleep(4)
    print 'starting audio player'
    time.sleep(10)
    def play_sound(filename):
        if security_status['silent_mode'] != True:
            try:
                sound = audio_p+' '+sound_dir+filename
                os.system(sound)
            except Exception as e:
                print e

    while True:
        if len(audio_queue) > 0:
            for audiofile in list(audio_queue):
                play_sound(audiofile)
                audio_queue.remove(audiofile)
        time.sleep(1)

#DAEMON THREAD
def notification_bot():
    #Thread voor te antwoorden op commands
    time.sleep(3)
    print 'starting notification bot'
    time.sleep(10)
    def readable_timedelta(then):
            now=time.time()
            td = timedelta(seconds=now - then)
            days, hours, minutes = td.days, td.seconds // 3600, td.seconds // 60 % 60
            text = '%s minutes' % minutes
            if hours > 0:
                text = '%s hours and ' % hours + text
                if days > 0:
                    text = '%s days ' % days + text
            return text

    while True:
        if len(message_queue) > 0:
            for message in list(message_queue):
                if message == '/reset':
                    update_log('message received: reset','event',False)
                    security_status['person_detected'] = False
                    security_status['alarm_triggered'] = False                    
                    message_queue.remove(message)
                elif message == '/sharp':
                    update_log('message received: sharp','event',False)
                    if security_status['sharp_mode'] == True:
                        security_status['sharp_mode'] = False
                        message_queue.remove(message)
                    else:
                        security_status['sharp_mode'] = True
                        message_queue.remove(message)
                elif message == '/silent':
                    update_log('message received: silent','event',False)
                    if security_status['silent_mode'] == True:
                        security_status['silent_mode'] = False
                        message_queue.remove(message)
                    else:
                        security_status['silent_mode'] = True
                        message_queue.remove(message)
                elif message == '/livestream':
                    update_log('message received: livestream','event',False)
                    if security_status['livestream_mode'] == True:
                        security_status['livestream_mode'] = False
                        message_queue.remove(message)
                    else:
                        security_status['livestream_mode'] = True
                        message_queue.remove(message)
                elif message == '/status':
                    update_log('message received: status','event',False)
                    send_notification(
                        'On time:'+readable_timedelta(security_status['time_start'])+
                        ',Current status:'+security_status['current_status']+
                        ',Previous status:'+security_status['previous_status']+
                        ',Last status change:'+readable_timedelta(security_status['last_status_change'])+
                        ',Livestream mode:'+str(security_status['livestream_mode'])+
                        ',Sharp mode:'+str(security_status['sharp_mode'])+
                        ',Silent mode:'+str(security_status['silent_mode'])+
                        ',Alarm triggered:'+str(security_status['alarm_triggered'])
                    )
                    message_queue.remove(message)
                elif message == '/sleep':
                    update_log('message received: sleep','event',False)
                    update_status('sleeping')
                    message_queue.remove(message)
                elif message == '/activate':
                    update_log('message received: activate','event',False)
                    update_status('watching')
                    message_queue.remove(message)
                elif message == '/photo':
                    update_log('message received: photo','event',False)
                    audio_queue.append('please_smile.mp3')
                    time.sleep(3)                    
                    take_photo()
                    message_queue.remove(message)
                elif message == '/deny':
                    update_log('message received: deny','event',False)
                    security_status['person_detected'] = False
                    audio_queue.append('Acces_denied.mp3')
                    message_queue.remove(message)
                elif message == '/accept':
                    update_log('message received: accept','event',False)
                    security_status['person_detected'] = False
                    unlock_door('admin')
                    message_queue.remove(message)
                elif message == '/off':
                    exit_out('Security system stopping...')
                    message_queue.remove(message)
        time.sleep(1)

def read_file(filename):
    with open(filename, 'rb') as f:
        photo = f.read()
    return photo

def take_photo():
        
    output_file = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')
    output_folder = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d')
    if not os.path.exists(host_dir+output_folder):
        os.makedirs(host_dir+output_folder)
    output_path = host_dir+output_folder+'/'+output_file+'.jpg'
    try:
        audio_queue.append('camera.mp3')        
        camera.capture(output_path)
        photo_queue.append(output_path)
    except Exception as e:
        print e
        update_log('failed to take photo','error',True)
    else:
        update_log('successfully took picture','event',False)


def send_confirmation_callback(message, result, user_context):
    global send_callbacks
    print(
        "Confirmation[%d] received for message with result = %s" %
        (user_context, result))
    map_properties = message.properties()
    print("    message_id: %s" % message.message_id)
    print("    correlation_id: %s" % message.correlation_id)
    key_value_pair = map_properties.get_internals()
    print("    Properties: %s" % key_value_pair)
    send_callbacks += 1
    print("    Total calls confirmed: %d" % send_callbacks)

def send_notification(message):
    #Stuur een notificatie
    try:
        
        pythonDictionary = {'deviceId':'SanderPi', 'message':message}
        dictionaryToJson = json.dumps(pythonDictionary)
        msg = IoTHubMessage(dictionaryToJson)
        
        iotHubClient.send_event_async(msg, send_confirmation_callback, 1)            
        update_log('notification send: '+message,'status update',False)
        
    except IoTHubError as e:
        update_log('unexpected error from IoT hub','error',True)

def blob_upload_confirmation_callback(result,user_context):
    global blob_callbacks
    print("Blob upload confirmation[%d] received for message with result = %s" % (user_context, result))
    blob_callbacks += 1
    print("    Total calls confirmed: %d" % blob_callbacks)
    update_log('uploaded new picture','event',True)
    #CHECK    

def upload_to_blob(filename,content):
    #Upload foto naar blob
    try:
        data = read_file(content)
        iotHubClient.upload_blob_async(filename,data,len(data),blob_upload_confirmation_callback,1001)        
    except Exception as e:
        print e
        update_log('unexpected error from IoT hub','error',True)

def receive_notification(message, counter):
    
    global receive_callbacks
    buffer = message.get_bytearray()
    size = len(buffer)
    plain_text = buffer[:size].decode('utf-8')
    counter += 1
    receive_callbacks += 1

    message_queue.append(plain_text)
    
    return IoTHubMessageDispositionResult.ACCEPTED

def trigger_alarm():
    audio_queue.append('Intruder_detected.mp3')
    update_log('intruder detected','alarm',True)
    send_notification('Intruder detected')    
    while security_status['alarm_triggered'] == True:        
        audio_queue.append('alarm.mp3')                
        time.sleep(10)

def motion_reset(sleep):
    global intro
    time.sleep(sleep)
    if security_status['current_status'] != 'sleeping':
        update_status('watching')
        intro = False

def motion_detected(channel):
    
    security_status['motion_detected']  = True
    if security_status['current_status'] != 'sleeping' and security_status['current_status'] == 'watching':                
            update_status('cautious')            
            timer_thread = Thread(target=motion_reset, args=(60,))
            timer_thread.daemon = True
            timer_thread.start()
                   
    
def exit_out(message):
    global sqlcon
    print message
    send_notification('security system stopping')
    update_log('security system stopped','event',True)
    sqlcon.close()
    GPIO.cleanup()
    camera.close()
    sys.exit(0)

if __name__ == "__main__":
    
    
    from picamera.array import PiRGBArray
    from picamera import PiCamera
    from PIL import Image
    import cv2
    import zbarlight
    import pymssql
    import iothub_client
    from iothub_client import *
    from iothub_client_args import *
    

    print 'Initialising...'

    #PIR init
    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BCM)

    #OpenCv init
    face_cascade = cv2.CascadeClassifier('/home/pi/opencv-2.4.10/data/haarcascades/haarcascade_frontalface_default.xml')
    eye_cascade = cv2.CascadeClassifier('/home/pi/opencv-2.4.10/data/haarcascades/haarcascade_eye.xml')
    
    message_queue = []
    qrcode_queue = []
    photo_queue = []
    audio_queue = []    

    #Camera init
    try:
        camera = PiCamera()
        camera.resolution = (cam_res_width , cam_res_height)
        camera.framerate = cam_framerate
        camera.led = False
        rawCapture = PiRGBArray(camera, size=(cam_res_width , cam_res_height))
    except Exception as e:
        print e
        exit_out('failed to initialise Pi camera.')
    
    #Iot hub init
    timeout = 241000
    minimum_polling_time = 9
    message_timeout = 10000
    receive_context = 0
    receive_callbacks = 0
    send_callbacks = 0
    blob_callbacks = 0

    protocol = IoTHubTransportProvider.HTTP
    
    try:
        (connection_string, protocol) = get_iothub_opt(sys.argv[1:], connection_string,protocol)
    except OptionError as o:
        print(o)
        exit_out('failed to connect to Iot hub.')
    try:
     iotHubClient = IoTHubClient(connection_string,protocol)       
     iotHubClient.set_option("timeout", timeout)
     iotHubClient.set_option("MinimumPollingTime", minimum_polling_time)
     # set the time until a message times out
     iotHubClient.set_option("messageTimeout", message_timeout)    
     iotHubClient.set_message_callback(receive_notification, receive_context)
    except IoTHubError as e:
        print e
        exit_out('failed to initialise Iot hub.')
    
    #sql
    try:
        sqlcon = pymssql.connect(server=sql_server, user=sql_user, password=sql_passw,database=sql_database)
        cursor = sqlcon.cursor(as_dict=True)
    except Exception as e:
        print e
        exit_out('failed to connect to sql db')
    
    security_status = {
        'time_start': time.time(),
        'current_status': 'sleeping',
        'previous_status': 'off',
        'last_status_change': time.time(),
        'livestream_mode' : False,
        'sharp_mode': False,
        'silent_mode': False,
        'alarm_triggered': False,
        'person_detected': False,
        'motion_detected': False,        
    }
    #Qr monitor
    qrcode_monitor_thread = Thread(name='qrcode_monitor',target=qrcode_monitor)
    qrcode_monitor_thread.daemon = True
    qrcode_monitor_thread.start()

    #Photo processor
    photo_processor_thread = Thread(name='photo_processor',target=photo_processor)
    photo_processor_thread.daemon = True
    photo_processor_thread.start()

    #Notifictaion bot
    notification_bot_thread = Thread(name='notification_bot',target=notification_bot)
    notification_bot_thread.daemon = True
    notification_bot_thread.start()
    
    #Audio player
    audio_player_thread = Thread(name='audio_player',target=audio_player)
    audio_player_thread.daemon = True
    audio_player_thread.start()
    
    time.sleep(7)
    try:
        GPIO.setup(output_pin1,GPIO.OUT)
        GPIO.setup(output_pin2,GPIO.OUT)
        GPIO.setup(pir_pin,GPIO.IN)
        GPIO.add_event_detect(pir_pin, GPIO.RISING, callback=motion_detected)
        
        print 'Security system running:'
        update_log('security system running','event',True)
        send_notification('Security system running')
        
        intro = False
        avg_background = None
        while True:
                #eenmaal doorlopen om te initialiseren                    
                for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
                    if security_status['current_status'] == 'cautious':
                        if GPIO.input(pir_pin) == 1:
                            security_status['motion_detected'] = True
                        elif GPIO.input(pir_pin) == 0:
                            security_status['motion_detected'] = False                 
                        image = frame.array        
                        gray = cv2.cvtColor(image,cv2.COLOR_BGR2GRAY)
                        gray_gaus = cv2.GaussianBlur(gray, (21, 21), 0)
                        #Qrcode detecteren enkel zwart wit nodig en laten verwerken door apart thread
                        qrcode_queue.append(gray)
                    
                        if avg_background is None:
                            avg_background = gray_gaus.copy().astype('float')
                            rawCapture.truncate(0)
                            continue
                        cv2.accumulateWeighted(gray_gaus, avg_background, 0.5)
                        frame_delta = cv2.absdiff(gray_gaus, cv2.convertScaleAbs(avg_background))
                        thresh = cv2.threshold(frame_delta,5, 255, cv2.THRESH_BINARY)[1]
                        thresh = cv2.dilate(thresh, None, iterations=2)
                        (contours, _) = cv2.findContours(thresh.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                        for cntr in contours:
                            if cv2.contourArea(cntr) < 5000:
                                continue
                            if intro != True and security_status['motion_detected'] == True and security_status['sharp_mode'] == False:
                                audio_queue.append('Hello.mp3')
                                audio_queue.append('qrcode.mp3')    
                                print 'Hello there'
                                intro = True
                            elif security_status['sharp_mode'] == True and security_status['motion_detected'] == True:
                                take_photo()                            
                                security_status['person_detected'] = True                              
                                if security_status['alarm_triggered'] != True:
                                    alarm_thread = Thread(name='alarm_thread',target=trigger_alarm)
                                    alarm_thread.daemon = True
                                    alarm_thread.start()
                                    security_status['alarm_triggered'] = True                                
                                
                        
                        if security_status['person_detected'] == False and security_status['sharp_mode'] == False:
                            #Open cv gezichts detectie
                            faceCount = 0
                            eyeCount = 0
                            faces = face_cascade.detectMultiScale(gray, 1.1, 5)
                            faceCount = len(faces)
                            for (x,y,w,h) in faces:
                                cv2.rectangle(image,(x,y),(x+w,y+h),(255,0,0),2)
                                roi_gray = gray[y:y+h, x:x+w]
                                roi_color = image[y:y+h, x:x+w]
                                eyes = eye_cascade.detectMultiScale(roi_gray)
                                eyeCount = len(eyes)
                           
                            if faceCount >= 1 and eyeCount >= 2 and security_status['motion_detected'] == True:                            
                                take_photo()
                                audio_queue.append('Person_detected.mp3')
                                audio_queue.append('qrcode.mp3')
                                send_notification('Person detected')
                                update_log('person detected','event',True)
                                security_status['person_detected'] = True
                    rawCapture.truncate(0)
                    #als de status op watching staat slapen ,maar alert zijn
                    while security_status['current_status'] == 'watching':
                        if intro != False:                            
                            intro = False
                        if security_status['person_detected'] != False:
                            security_status['person_detected'] = False   
                        time.sleep(2)
                    #als de status op sleeping staat slapen ,zonder alert te zijn
                    while security_status['current_status'] == 'sleeping':
                        time.sleep(10)        
    except KeyboardInterrupt:
        exit_out('Security system stopping...')