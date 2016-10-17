import network
import machine
import socket
import ssd1306
import time
import utime
import ntptime
from machine import RTC,I2C,Pin

#Configuration of the WiFi
def do_connect():
        global sta_if
	sta_if = network.WLAN(network.STA_IF)
	if not sta_if.isconnected():
		print('connecting to network...')
		sta_if.active(True)
		sta_if.connect('Columbia University', '')
		#sta_if.connect('GOODGOODSTUDY','daydayup')
		while not sta_if.isconnected():
			pass
	print('network config:', sta_if.ifconfig())

def displaycontent(str):
        display.fill(0)
        display.text(str,5,5,1)
        display.show()

def showtime():
        global t,h
        t=ntptime.time()
        t=utime.localtime(t)
        if t[3]>=4:
                h=t[3]-4
        else:
                h=t[3]+20
        b=str(h)+':'+str(t[4])+':'+str(t[5])
        displaycontent(str=b)
        
do_connect()
http = b'HTTP/1.1 200 OK\r\ncContent-Type: text/html\r\n\r\n<html>\r\n<head>\r\n<title>YKY Homepage</title>\r\n</head>\r\n<body>welcome to Smart Watch!\r\n</body>\r\n</html>\r\n\r\n'
#webpage of the server
ser=socket.socket()
addr,_,_,_ = sta_if.ifconfig()
addr=(addr,80)
ser.bind(addr)                   #keep esp listening on the server
print("listening on:",addr)
ser.listen(1)
rtc=RTC()
i2c=I2C(scl=Pin(5),sda=Pin(4))
display=ssd1306.SSD1306_I2C(128,32,i2c,60)
led=Pin(0,Pin.OUT)
led.high()
time_running=0
connect=0
while 1:
        ser.settimeout(0.5)
        try:
                cli,add_c=ser.accept()     #get socket from the client end
                connect=1
        except OSError:
                if time_running==1:
                        now=rtc.datetime()
                        now=(now[4],now[5],now[6])
                        displaycontent(str(now[0])+":"+str(now[1])+":"+str(now[2]))
                        connect=0
                else:
                        connect=0
        if connect==1:
                print("connected by:",add_c)
                test=cli.recv(512)
                test=bytes.decode(test)
                if not test.find('api_command') == -1:
                        _,command = test.split('api_command',2)
                        _,command = command.split(':',2)
                        command,_ = command.split('}',2)
 #                       _,command,_ = command.split('\\',3)
                        displaycontent(command)
                        time_running=0
                        time.sleep_ms(100)
                        if not command.find('turn off') == -1:
                                display.fill(0)
                                displaycontent('')
                        elif not command.find('time') == -1:
                                showtime()
                                t=(t[0],t[1],t[2],1,h,t[4],t[5],t[6])
                                rtc.datetime(t)
                                time_running=1
                cli.send(http)
                connect=0
                cli.close()
