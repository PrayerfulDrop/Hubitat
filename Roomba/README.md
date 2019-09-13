<h1>Roomba for Hubitat</h1><br>
<p>Roomba for Hubitat is a forked version from the original author Dominick Meglio.  This forked version enables advanced scheduling, notification and 
support for all WiFi enabled Roomba devices.</p>
<br>
<h2>Prerequisites and installation:</h2>
<p>Roomba for Hubitat uses <a href="https://github.com/koalazak/dorita980">Dorita980</a> for Roomba control and <a href="https://github.com/koalazak/rest980">Rest980</a> for API control and connectivity into Hubitat using a Rapsberry Pi.  These directions will be how to install both on a RPi currently running Node.JS and GIT installed.  I would recommend using a /share directory for this installation.</p>
<b>Dorita980 and Rest980 Installation process:</b>
<p>
<ol>
  <li>sudo npm install -g dorita980 --save</li>
  <li>sudo npm -g install dorita980</li>
  <li>Install Rest980
    <ul><li>git clone https://github.com/koalazak/rest980.git</li>
    <li>cd rest980</li>
    <li>npm install</li></ul></li>
</ol>
</p>
<b>Rest980 Configuration (recommended to have two SSH windows open):</b>
<ul>
  <li>[SSH Window #1] Edit ../rest980/config/default.json</li>
  <li>[SSH Window #2]get-roomba-password <YOUR_ROOMBA_IP_ADDRESS>
  <ul><li>On your Roomba device that is on the Home Base and powered on, press and hold your Roomba's Home button for at least two seconds until you hear a beep or a series of tones</li>
  <li>Press and key in the SSH Windows #2 to continue</li></ul>
  <li>Copy "blid" and past into [SSH Window #1] for <b>blid</b></li>
  <li>Copy the "password" revealed and past into [SSH Windows #1] for <b>password</b></li>
  <li>Enter Roomba IP Address into [SSH Windows #1] for <b>robotIP</b></li>
  <li>Change "firmwareVersion" in [SSH Windows #1] to <b>1</b></li>
    <li>Logout of [SSH Window #2]</li>
    <li>Testing:<ul>
      <li>Navigate back to ../rest980 root</li>
      <li>Type the following: DEBUG=rest980:* npm start</li>
      <li>In a new browser tab navigate to http://YOUR_RASPBERRY_PI:3000</li>
      <li>Response should be similar to: {"documentation":"https://github.com/koalazak/rest980","pong":"2019-09-13T12:13:36.408Z"}</li>       <li>CTRL-C to exit Rest980 app</li></ul>
</ul>
    <b>Setting up Rest980 to run as a service:</b>
    <ul><li>sudo nano /etc/systemd/system/roomba.service</li>
      <li>Enter the following changing your WorkingDirectory to where yours is:<br><p>
[Unit]
Description=Roomba Service
After=network.target

[Service]
WorkingDirectory=/share/rest980
ExecStart=/usr/bin/npm start
Restart=on-failure
User=pi

[Install]
WantedBy=multi-user.target
</p></li>
<li>sudo systemctl enable roomba.service</li>
<li>sudo systemctl start roomba.service</li>
<li>Test connectivity in browser tab: http://YOUR_RASPBERRY_PI:3000</li>
<li>Response should be similar to: {"documentation":"https://github.com/koalazak/rest980","pong":"2019-09-13T12:13:36.408Z"}</li>       
  </ul>
  
<b>Install Hubitat App and Driver:</b>
<ul>
  <li>Roomba Application install:
    <ul>
      <li>Copy Roomba-app.groovy RAW URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-app.groovy
      <li>In your Hubitat Administrative Console select Apps Code</li>
      <li>Click New App in upper right corner</li>
      <li>Click Import</li>
      <li>Paste RAW URL</li>
      <li>Click Save</li>
    </ul>
  </li>
  <li>Roomba Device install:
    <ul>
      <li>Copy Roomba-driver.groovy RAW URL: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/Roomba/Roomba-device.groovy
      <li>In your Hubitat Administrative Console select Drivers Code</li>
      <li>Click New Driver in upper right corner</li>
      <li>Click Import</li>
      <li>Paste RAW URL</li>
      <li>Click Save</li>
    </ul>
  </li>
</ul>

    