#!/usr/bin/python

import usb.core
import sys
import time
import random
import json

VID_NEXUS_4_DEBUG = 0x18d1
PID_NEXUS_4_DEBUG = 0xd002

VID_ANDROID_ACCESSORY = 0x18d1
PID_ANDROID_ACCESSORY = 0x2d01

def get_accessory():
    print('Looking for Android Accessory')
    print('VID: 0x%0.4x - PID: 0x%0.4x'
        % (VID_ANDROID_ACCESSORY, PID_ANDROID_ACCESSORY))
    dev = usb.core.find(idVendor=VID_ANDROID_ACCESSORY, 
                        idProduct=PID_ANDROID_ACCESSORY)
    return dev

def get_android_device():
    print('Looking for Nexus 4')
    print('VID: 0x%0.4x - PID: 0x%0.4x'
        % (VID_NEXUS_4_DEBUG, PID_NEXUS_4_DEBUG))
    android_dev = usb.core.find(idVendor=VID_NEXUS_4_DEBUG, 
        idProduct=PID_NEXUS_4_DEBUG)
    if android_dev:
        print('Nexus 4 (debug) found')
    else:
        sys.exit('No Android device found')
    return android_dev


def set_protocol(ldev):

    try:
        ldev.set_configuration()
    except usb.core.USBError as e:
        if  e.errno == 16:
            print('Device already configured, should be OK')
        else:
            sys.exit('Configuration failed')

    ret = ldev.ctrl_transfer(0xC0, 51, 0, 0, 2)

# Dunno how to translate: array('B', [2, 0])
    protocol = ret[0]

    print('Protocol version: %i' % protocol)
    if protocol < 2:
        sys.exit('Android Open Accessory protocol v2 not supported')

    return

def set_strings(ldev):
    send_string(ldev, 0, 'sankovicmarko.com')
    send_string(ldev, 1, 'USBAccessoryService')
    send_string(ldev, 2, 'USBAccessoryServiceDescription')
    send_string(ldev, 3, '0.0.1')
    send_string(ldev, 4, 'https://usbaccessoryservice.sankovicmarko.com')
    send_string(ldev, 5, 'USBAccessoryServiceSerial')    
    return

def set_accessory_mode(ldev):
    ret = ldev.ctrl_transfer(0x40, 53, 0, 0, '', 0)    
    if ret:
        sys.exit('Start-up failed')
    time.sleep(1)
    return

def send_string(ldev, str_id, str_val):
    ret = ldev.ctrl_transfer(0x40, 52, 0, str_id, str_val, 0)
    if ret != len(str_val):
        sys.exit('Failed to send string %i' % str_id) 
    return 

def start_accessory_mode():
    dev = get_accessory()
    if not dev:
        print('Android accessory not found')
        print('Try to start accessory mode')
        dev = get_android_device()
        set_protocol(dev)
        set_strings(dev)
        set_accessory_mode(dev)
        dev = get_accessory()
        if not dev:
            sys.exit('Unable to start accessory mode')
    print('Accessory mode started')
    return dev

def sensor_variation(toss):
    return {
        -10: -1,
        10: 1
    }.get(toss, 0)

def sensor_output(lsensor, variation):
    output = lsensor + variation
    if output < 0:
        output = 0
    else:
        if output > 100:
            output = 100
    return output

def wait_for_command(ldev):
    sensor = 50
    while True:
        try:
            toss = random.randint(-10, 10)
            if sensor + sensor_variation(toss) in range(0, 101):
                sensor = sensor + sensor_variation(toss)
            print ('Sensor: %i' % sensor)
            msg = ('S%0.4i' % sensor)
            # payload = {
            #    "method": "speed",
            #    "params": 123,
            #    "jsonrpc": "2.0",
            #    "id": 0
            # }
            # msg = json.dumps(payload)
            print('<<< ' + msg),
            try:
                ret = ldev.write(0x02, msg, 0, 150)
                if ret == len(msg):
                    print(' - Write OK')
            except usb.core.USBError as e:
                print e

            print('>>> '),
            try:
                ret = ldev.read(0x81, 5, 0, 150)
                sret = ''.join([chr(x) for x in ret])
                print sret
                if sret == "A1111":
                    variation = -3
                else:
                    if sret == "A0000":
                        variation = 3 
                sensor = sensor_output(sensor, variation)
            except usb.core.USBError as e:
                print e
            time.sleep(0.2)
        except KeyboardInterrupt:
            print "Bye!"
            break
        

#        msg='test'

    return

# Define a main() function 
def main():

    dev = start_accessory_mode()
    wait_for_command(dev)

# 'This is the standard boilerplate that calls the main() function.
if __name__ == '__main__':
    main()
