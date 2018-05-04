# First Hack - Data Shapes and Data Mapper
-Instructor lead-

Publishing input and receiving from announcement topic!
![Working with GUI](docs/images/hack-01-01.png)
![Working with GUI](docs/images/hack-01-02.png)

- Add connection for messaging broker for Input queue and announcement topic
Select the **Connections** on the side menu, and click on **Add Connection** button on the top right hand corner.
![AMQP Connection Setup](docs/images/amqp-connection-01.png)

- Click on the AMQP Connector.
![AMQP Connection Setup](docs/images/amqp-connection-02.png)

- Configure your AMQP settings accordingly, all the information should be avaliable in your OSE env, within the Broker's secret setting. 

   	- Connection URL : amqp://*messagingHost* 
   	- User Name: *username*
   	- Password: *password*
   	- Check Certificate: *Disable*

- And click **Next** when done. 

![AMQP Connection Setup](docs/images/amqp-connection-04.png)
![AMQP Connection Setup](docs/images/amqp-connection-03.png)

- Give a name to your broker connection, and click **create** 
![AMQP Connection Setup](docs/images/amqp-connection-05.png)

- **Create new integration**. Select the **Integration** on the side menu, and click on **Create Integration** in the center.

![Integration hack one](docs/images/hackone-integration-01.png)

- Select < YOUR_BROKER_CONNECTION > .
![Integration hack one](docs/images/hackone-integration-02.png)

- Select *subscribe for messages action*. 
![Integration hack one](docs/images/hackone-integration-03.png)

- Configure the name of the queue to listen
	- Destination Name: inputs
	- Destination Type: Queue 
![Integration hack one](docs/images/hackone-integration-04.png)

- Configure the Output data type,  
  	- Select Type: JSON Instance
	- Definition: 
	
```
{
  "type": "announcement",
  "content": {
    "title": "Tester",
    "text": "This is the message for everyone!!"
  }
}
```
![Integration hack one](docs/images/hackone-integration-05.png)

- For end connector setting, and select the <YOUR_BROKER_CONNECTION>   
![Integration hack one](docs/images/hackone-integration-06.png)

- Select *Publish messages action*. 
![Integration hack one](docs/images/hackone-integration-07.png)

- Configure the name of the queue to listen
	- Destination Name: notifications
	- Destination Type: Topic 
![Integration hack one](docs/images/hackone-integration-08.png)

- Configure the Output data type,  
  	- Select Type: JSON Instance
	- Definition: 
	
```
{
  "type": "Success",
  "header": "Christina",
  "message": "This is the message for <strong>everyone</strong>!!"
}
```	
![Integration hack one](docs/images/hackone-integration-09.png)

- Select *Add a Step* in the center
![Integration hack one](docs/images/hackone-integration-10.png)

- Select *Data Mapper* in action
![Integration hack one](docs/images/hackone-integration-12.png)

- Add two contant by clicking in the Source column **"+"** of *Constants* drop down menu. 
	- <YOUR_NAME>
	- Warning 
![Integration hack one](docs/images/hackone-integration-11.png)

- Drag and connect from Source to Target and click in **Done**
	- Custom -> Content -> text **to** message
	- Custom -> Content -> title  **to** header
	- Constants -> Warning **to** type 
![Integration hack one](docs/images/hackone-integration-13.png)

- Give a name to integration and click in **Finish**
![Integration hack one](docs/images/hackone-integration-14.png)

- Go to your UI, *http://www-hackathon-ui-<USER_NAME>.apps.<ID>.openshift.opentlc.com/*
,in the input panel, 
	- Tyoe: Annoucement
	- Titles: < YOUR_NAME >
	- Text: Hello

![Integration hack one](docs/images/hackone-integration-15.png)

- You should see a small alert pop up on the top righthand corner.

![Integration hack one](docs/images/hackone-integration-16.png)