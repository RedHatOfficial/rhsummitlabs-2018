#!/bin/bash
/bin/pkill -f Xvnc
/bin/pkill -f novnc
/bin/rm -Rf /tmp/.X11-unix/ /home/student/.vnc/*pid
/sbin/runuser -l student -c "/usr/bin/vncserver :1 -geometry 1024x768 -localhost >/dev/null 2>&1 &"
/bin/websockify --web=/usr/share/novnc/ --cert=/etc/certs/certs/novnc.crt --key=/etc/certs/private/novnc.key 9000 localhost:5901 >/dev/null 2>&1 &
