# Setup API Services

## Import API Client Connectors
To import custom API connector, this will allow you to utilize the SaaS API avalible on the internet. Go to Customizations on the side navigation menu.

On the **API Client Connector** tab, click on **Create API Connecot** 

![Import Custom API Connector](docs/images/api-connector-customize-01.png)

In the **Upload Swagger Specification**, select the *Use a URL* option and enter the following URL in the URL location to create the connector, and click **Next**.

```
https://raw.githubusercontent.com/weimeilin79/summitlab2018/master/swaggerdocs/ParkingLocationService.yaml
```

![Import Custom API Connector](docs/images/api-connector-customize-02.png)

Click **Next**,

![Import Custom API Connector](docs/images/api-connector-customize-03.png)

Click **Next**,

![Import Custom API Connector](docs/images/api-connector-customize-04.png)

Click **Create Connector**,

![Import Custom API Connector](docs/images/api-connector-customize-05.png)

And you will see the Connector that you just added.
![Import Custom API Connector](docs/images/api-connector-customize-06.png)

Add the following four API connectors. 

```
#ATM Location
https://raw.githubusercontent.com/weimeilin79/summitlab2018/master/swaggerdocs/ATMLocationService.yaml

#BAR Location
https://raw.githubusercontent.com/weimeilin79/summitlab2018/master/swaggerdocs/BarLocationService.yaml

#Restaurant Location
https://raw.githubusercontent.com/weimeilin79/summitlab2018/master/swaggerdocs/RestaurantLocationService.yaml

#Store Location
https://raw.githubusercontent.com/weimeilin79/summitlab2018/master/swaggerdocs/StoreLocationService.yml
```
## Create API Connections

Now it's time to create a Connection, select the **Connections** on the side navigation menu. On the top right hand corner, click on **Create Connection**.

![Create API Connections](docs/images/create-connection-01.png)

Select *San Fransisco Parking API* Connecotr

![Create API Connections](docs/images/create-connection-02.png)

In the configuration setup page, and place **/** in the **base path**. And click on **Next**.

![Create API Connections](docs/images/create-connection-03.png)

Set Connection Details, add **parking-sf** in the Connection Name and **San Fransisco API** in the description. And click on **Create**.

![Create API Connections](docs/images/create-connection-04.png)

And you will see the connection that just created.
![Create API Connections](docs/images/create-connection-05.png)

Do the same to other four API connectors.

- ATM Location
- BAR Location
- Restaurant Location
- Store Location