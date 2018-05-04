# Messaging using OpenShift Service Catalog

This tutorial walks you through provisioning messaging infrastructure and deploying the example messaging application using the OpenShift Service Catalog.

## Provision Address Space

1. In the OpenShift Service Catalog overview, select **EnMasse (brokered)**.

    ![Catalog](docs/images/messaging-01.png)

1. Click the **Next** button to start the configuration.

    ![Information](docs/images/messaging-02.png)

1. Select your project in the **Add to Project** field. If you haven't yet created a projet, select the **Create Project** in the drop-down box. Use your username as the project name _userX_. Use the same value for the **name** field. 
1. Click the **Next** button.

    ![Configuration](docs/images/messaging-03.png)

1. Select the **Do not bind at this time** option. Click the **Create** button.

    ![Binding](docs/images/messaging-04.png)

1. The provisioning has been scheduled now. Click on the **Continue to the project view** link to review the progress.

    ![Results](docs/images/messaging-05.png)

1. The address space will be provisioned and may take a few
minutes.

    ![Provisioning](docs/images/messaging-06.png)

## Configure Addresses

1. Collapse the Service by clicking on the header.

    ![Service](docs/images/messaging-07.png)

1. Log in to the Messaging console by clicking on the **Dashboard** link. It will open a new tab.

    ![Dashboard link](docs/images/messaging-08.png)

1. The link will redirect you to the Single Sign On Login Page. Click the **OpenShift** button.

    ![SSO](docs/images/messaging-09.png)

1. This will redirect you to the OpenShift login. Type in your assigned username and password and click the **Log In** button.

    ![Login](docs/images/messaging-10.png)

    If everything is ok you will land in the _Dashboard_ page of the messaging console.
    
    ![Dashboard](docs/images/messaging-11.png)

1. Click the **Addresses** option on the left side menu.

1. To start creating the addresses, click the **+ Create** button.

    ![Create](docs/images/messaging-12.png)

1. Fill in the **Name** with the address name like *inputs* and select the **Type**, in this case *queue*. Click on **Next >**.

    ![Definition](docs/images/messaging-13.png)

1. Select the default **Plan** type and click on **Next >**.

    ![Plan](docs/images/messaging-14.png)

1. Finally click on **Create**.

    ![Summary](docs/images/messaging-15.png)

1. Your address will be provisioned in the address space and will become available shortly.

    ![Address](docs/images/messaging-16.png)

1. Add the following addresses to your space:

    * **notifications** - type: *topic*
    * **inputs** - type: *queue*
    * **locations** - type: *queue*
    * **temp1** - type: *queue*
    * **temp2** - type: *queue*
    * **temp3** - type: *queue*

    You will be able to create additional addresses if needed.

## Create Application Credentials

Now you are able to provision credentials for your applications. You will need this information when creating the Web Application UI. Follow the next steps to create an app binding.

1. Get back to the OpenShift web console.

1. Click on **Create Binding** link of the messaging service. Wait a few seconds to let the wizard dialog load completly.

    ![CreateBinding](docs/images/messaging-17.png)

1. Click **Next >**.

    ![Binding](docs/images/messaging-18.png)

1. Select the **externalAccess** checkbox and leave the default options. Click **Bind**.

    ![Parameters](docs/images/messaging-19.png)

1. Click **Close** to finish the process.

    ![Results](docs/images/messaging-20.png)

1. Expand the messaging service and click **View Secret** of the newly created binding.

    ![View Secret](docs/images/messaging-21.png)

1. Click **Reveal Secret** to show the credentials and extra information. You will need this information for the Web Application UI.

    ![Reveal Secret](docs/images/messaging-22.png)

1. Take notice of the **externalMessagingHost**, **externalMessagingPort**, **password**, and **username** you will need those values to connec the web application UI to the messaging service.

    ![Credentials](docs/images/messaging-23.png)

## Summary

In this tutorial, you've seen how to provision messaging using the OpenShift Service Catalog. You
have seen how to bind it to the provisioned messaging service. You then used the messaging console to create an address before modifying the application to use the secret for authenticating.