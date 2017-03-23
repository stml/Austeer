import cv2
import csv
import numpy as np
from datetime import timedelta
import time
from PIL import Image
from PIL import ImageFont
from PIL import ImageDraw 

data = []

height = 720
width = 1280

# Load dashcam video. This should be clipped to start and end of data.
cap = cv2.VideoCapture('MountainDataSynced.mp4')

# get fps of input video and calculate frame interval in ms
fps = cap.get(cv2.cv.CV_CAP_PROP_FPS)
framestep = 1000/fps

# Font for data writing
font = ImageFont.truetype("sourcecode.otf", 16)

# csv for Austeer Output file
with open('austeer-output.csv', 'rb') as csvfile:
    reader = csv.reader(csvfile, delimiter=',', quotechar='|')
    for row in reader:
        data.append(row)
    
# save output movie in local location with timestamp    
fourcc = cv2.cv.CV_FOURCC('m', 'p', '4', 'v')
out = cv2.VideoWriter('output-%f.mp4' % (time.time()),fourcc, fps, (width,height),True)

framecount = 0

# pointer to current row in data
pointer = 0

# Iterate through frames
while(cap.isOpened()):

	# elapsed time in video
    elapsed = framecount*framestep
    
    # check if next datapoint has been passed
    nexttime = data[pointer+1][0]
    nexttime_a = nexttime.split(':')
    seconds = nexttime_a[2].split('.')
    d_nexttime = int(nexttime_a[0])*3600000 + int(nexttime_a[1])*60000 + int(seconds[0])*1000 + int(seconds[1])
    if elapsed > d_nexttime:
        pointer = pointer + 1

	# get the next frame
    ret, frame = cap.read()
    
    # For some reason, overlay image needs to be opened afresh each cycle, otherwise it becomes corrupted
    wheel = Image.open('steeringwheel.png')
    wwidth, wheight = wheel.size
    
    # Orient steering wheel to data
    angle = -float(data[pointer][4])*180
    rot = wheel.rotate( angle, resample=Image.BICUBIC)
    
    # Convert frame to RGB space (from numpy array BGR) and PIL image
    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    img = Image.fromarray(frame, 'RGB')  
    
    # Draw steering wheel on frame
    img.paste( rot, (width/2 - wwidth/2, height/2 - wheight/2), rot )
    
    # draw data to screen
    draw = ImageDraw.Draw(img)
    draw.text((10, 694),'Data Time Elapsed: '+data[pointer][0],(255,255,255),font=font)
    draw.text((10, 10),data[pointer][1]+','+data[pointer][2],(255,255,255),font=font)   
    draw.text((10, 30),'Speed: '+data[pointer][3],(255,255,255),font=font) 
    draw.text((10, 50),'Angle: '+data[pointer][4],(255,255,255),font=font)
    draw.text((10, 668),'Video Time Elapsed: '+str(timedelta(seconds=elapsed/1000)),(255,255,255),font=font)
    
    # Comment out this line to hide processing progress
    print('Video Time Elapsed: '+str(timedelta(seconds=elapsed/1000)))
    
    # Uncomment this line to save individual frames as images
    #img.save('frame-'+str(framecount).zfill(8)+'.png')
    
    # Convert final frame back to nimpy array / BGR
    imgx = np.array(img)
    imgx = cv2.cvtColor(imgx, cv2.COLOR_RGB2BGR)
    
    # Uncomment this line to see visual progress in new window
    #cv2.imshow('Ascent',imgx)
    
    # Write Video Frame
    out.write(imgx)
    
    # Update frame counter
    framecount = framecount + 1
    
    # Loop can be broken by 'q' in Python, or Ctrl-C in terminal
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# End all processes
cv2.destroyAllWindows()
cap.release()
out.release()
