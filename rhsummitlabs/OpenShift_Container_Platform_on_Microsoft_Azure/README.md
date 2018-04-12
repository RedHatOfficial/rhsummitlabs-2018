# Deploying OpenShift Origin on Microsoft Azure

## Part 0: Prework and starting with Microsoft Azure
**Estimated time: 35 minutes**

The prework for this lab will walk you through the Azure sign-up process, as
well as the preliminary steps of setting up Cloud Shell and provisioning your
first RHEL VM in your new subscription. Full completion of this prework is
necessary not only to gain basic experience with Azure, but to ensure that
the time-consuming "first run" processes happen for your Azure account before
the lab and not during the lab. This will allow us to focus Lab time on the
intended Lab content. Please complete ALL of Section 0 before coming to the Lab.

**IMPORTANT NOTE:** You must email <openshiftrunsonazure@microsoft.com> for an Azure Pass
as part of this prework. Send an email with your first and last name to 
<openshiftrunsonazure@microsoft.com> requesting that you be provisioned an Azure
Pass and you will receive one within 24 hours. Seriously, stop reading this and
email us RIGHT NOW! We promise to respond as soon as possible, but we sometimes
sleep so we might not respond as quickly to 3am emails. Once you have that pass,
you may complete the rest of the prework.

### 0.1: Signing up for Azure (estimated time: 5 minutes)
You have now presumably received an Azure Pass from us. This pass contains 
$250USD of credit for Azure services and is good for 1 month. Any unused credit
at the end of the month will expire and all paid services will be stopped. You
will have the option of converting the free trial to one of the paid options at
the end of the free term, or you can just let it expire. Your choice!

You will need a Microsoft Account and a credit card to sign up for the free 
trial. The Microsoft Account is required to access Azure services, and your
credit card is only used to verify your identity. In other words, your credit
card will not be charged unless you explicitly allow it.

> NOTE: 
> If you have used any Microsoft Services such as OneDrive, Office365, Outlook.com,
> Hotmail.com, XBox, Skype, or Bing Rewards, or have a @outlook.com, @hotmail.com,
> @live.com email address, then you probably have a Microsoft Account. If you 
> don't have a Microsoft account, go to 
> [https://account.microsoft.com](/uri "https://account.microsoft.com") and sign
> up for an account there.

Once you have a Microsoft account, you will need to sign up for Azure. To do this,
go to [https://azure.microsoft.com/free](/uri "https://azure.microsoft.com/free"),
select "Start Free", and follow the instructions there.

### 0.2: Redeeming an Azure Subscription using your Azure Pass (estimated time: 15 minutes)
The Azure Pass is a special code that gives you an Azure subscription very
similar to the Azure Free Trial. The Azure Pass does NOT require a credit card 
to redeem. 

Once you have the code, start the Azure Pass redemption process at 
[https://www.microsoftazurepass.com/](/uri "https://www.microsoftazurepass.com/")
and follow the steps there. Further instructions are available at 
[https://www.microsoftazurepass.com/Home/HowTo](/uri "https://www.microsoftazurepass.com/Home/HowTo").

### 0.3: Start Cloud Shell for the first time (estimated time: 2 minutes)
In the Lab, we will be using the Azure Cloud Shell as our primary means of
deployment. For prework, you will merely open Cloud Shell for the first time.
Doing so will ensure that you have Cloud Shell properly set up in your Azure
Account. 

Once you have signed up for Azure and redeemed your Azure Pass, visit
[https://shell.azure.com](/uri "https://shell.azure.com"). When prompted, select
Bash as your shell of choice. Once your Cloud Shell instance has loaded, feel
free to close it as you have completed this step now. Or play around with it if
you so choose :smiley:.

### 0.4: Provision a RHEL VM through the Portal UI (estimated time: 10 minutes)
The final prework step is to provision a RHEL VM using the Azure Portal UI. This
will ensure that everything with your Azure Pass and Azure Account is working
properly before the lab. This should take no longer than a few minutes. Begin by
navigating to [https://portal.azure.com](/uri "https://portal.azure.com").

1. Click on "Create a resource" in the top left corner of the Portal
1. Search for "RHEL 7.4" in the search box
1. Select "Red Hat Enterprise Linux 7.4"
1. In the dropdown, select "Resource Manager" as the deployment model. You will
be taken to the Create virtual machine options
1. In the "Configure basic settings" step, enter the following:
    1. Name: Any arbitrary name
    1. User name: Any arbitrary username
    1. Authentication type: Password, and choose a password of your liking
    1. Subscription: Ensure this has selected the subscription you have redeemed
    with your Azure Pass.
    1. Resource group: Choose "Create new", and give it any arbitrary name
    1. Location: West US 2
    1. Click "OK"
1. In the "Choose a size" step, choose B1s as your VM size (you are free to
spin up a larger VM size, but please note that this might mean that you don't
have enough remaining credits to do the actual Lab). Once you have selected B1s, 
click the "Select" button.
1. In the "Configure optional features" step, you don't have to change anything.
Read it over if you would like, and click "OK".
1. In the Summary step, you can view a summary of the VM you are about to spin up.
Have a look and ensure you like what you see, then click "Create". Creation will
only take a few minutes (in our testing, it took 3 minutes). You will be
redirected back to the Portal dashboard where a tile will appear to indicate
that deployment has begun.
1. Do a happy dance because you just created your first RHEL VM on Azure!

### 0.5: Clean up resources (estimated time: 2 minutes)
Remember when we told you above that your Azure Pass only contains $250USD of
credit? Well we wouldn't want you to use it all up before we even start the Lab,
so let's clean up everything we just created so you still have ample credit
during the lab. Azure allows for quick deletion of resources grouped together
through the idea of Resource Groups. 

1. From the left side of the Portal, click on "Resource groups"
1. Select the Resource Group name that you entered in during your VM creation
1. Click on the "Delete resource group" button
1. Type in the Resource Group name in the text box to tell Azure you're really
sure about deleting it, then click Delete.
1. You're done with the Prework! 

__*Stop here now that you're done the Prework. Everything that follows below will
be completed during the Red Hat Summit Lab itself.*__
---

## Part 1: Using Azure to deploy OpenShift Origin
### 1.0: Open cloud shell and log in with your Azure account you created in the Prework
In case you forgot, here's a helpful link:
[https://shell.azure.com](/uri "https://shell.azure.com")

### 1.1: Create a new Resource Group with `azure group create`
This resource group will be used to host your Azure key vault. You will deploy a
separate resource group for your OpenShift cluster resources.
```bash
> azure group create --name <KEYVAULT_RESOURCE_GROUP_NAME> --location westus2
```
**Pro Tip:** You can be even lazier with what you type by typing `az` instead of `azure`.
The rest of this guide will use `az` instead of `azure` for Azure CLI commands.

### 1.2: Get your subscription's SubscriptionId with `az account list`
```bash
> az account list
```

### 1.3: Generate SSH Keys with `ssh-keygen`
```bash
> ssh-keygen
```
Ensure that no passphrase is created for the key, and note down where the key
pair is saved (probably ~/.ssh/).

### 1.4: Create your Azure Service Principal
```bash
> az ad sp create-for-rbac \
    --name <YOUR_NAME_HERE> \
    --password <YOUR_PASSWORD_HERE> \
    --role contributor \
    --scopes /subscriptions/<YOUR_SUBSCRIPTION_ID>/resourceGroups/<YOUR_RESOURCE_GROUP_NAME> 
```
Your output will look something like this:
```json
{
  "appId": "11111111-abcd-1234-efgh-111111111111",            
  "displayName": "<YOUR_NAME_HERE>",
  "name": "http://<YOUR_NAME_HERE>",
  "password": <YOUR_PASSWORD_HERE>,
  "tenant": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
}
```
**Notes:**  
  * Azure CLI commands are sorted hierarchically. In this case, `az ad sp create-for-rbac`
  expands to "Azure -> Active Directory -> Service Principal -> Create for Role-
  Based Access". 
  * Your SubscriptionId can be found with `az account list` (from above).
  * Your Resource Group name can be found with `az group list`.
  * The final `scopes` argument limits the scope of the Service Principal to the
  current Resource Group. This is a generally accepted best practice.

### 1.5: Create your Azure Key Vault
Create a key vault to store the SSH keys for the cluster with the az keyvault
create command. The key vault name must be globally unique.
```bash
> az keyvault create \
    --resource-group <KEYVAULT_RESOURCE_GROUP_NAME> \
    --name <KEYVAULT_NAME> \
    --location westus2 \
    --enabled-for-template-deployment true
```
### 1.6: Store your SSH private key in the Azure Key Vault
The OpenShift deployment uses the SSH key you created to secure access to the OpenShift master. To enable the deployment to securely retrieve the SSH key, store the key in Key Vault.
```bash
> az keyvault secret set \
    --vault-name <KEYVAULT_NAME> \
    -- name <SECRET_NAME> \
    --file <PATH_TO_SSH_PRIVATE_KEY> 
```
**Note:**
  * The path to your SSH private key is probably ~/.ssh/id_rsa

### 1.7: Download the OpenShift deployment repository
You can use one of two ways to deploy OpenShift Origin on Azure:

  * You can manually deploy all the necessary Azure infrastructure components, and
then follow the OpenShift Origin documentation.
  * You can also use an existing Resource Manager template that simplifies the
deployment of the OpenShift Origin cluster.

Today, we will be using the template to automate the deployment of all the
necessary Azure infrastructure components and run the setup scripts required.

1. Go to [https://aka.ms/openshift](/uri "https://aka.ms/openshift") and clone
the repo to your local machine (in today's lab, this can all be done within Azure
Cloud Shell).
1. Enter the cloned repo directory and create copies of `azuredeploy.json` and
`azuredeploy.parameters.json`
    ```bash
    > cd openshift-origin
    > cp azuredeploy.json azuredeploy.local.json
    > cp azuredeploy.parameters.json azuredeploy.parameters.local.json
    ```
1. Use your preferred text editor to make adjustments to both files. We will be
updating `azuredeploy.local.json` to allow for fewer Master nodes. This is due
to a 10 core constraint in the Azure Pass. The bulk of our editing work will be
in `azuredeploy.parameters.local.json`. There are a number of fields tagged with
a "changeme" string that will need to be edited.
    #### Changes required for `azuredeploy.local.json`:
    ```json
    "masterInstanceCount": {
        "type": "int",
        "defaultValue": 3,
        "allowedValues": [1,5],
        "metadata": {
            "description": "Number of OpenShift masters."
        }
    },
    "infraInstanceCount": {
        "type": "int",
        "defaultValue": 2,
        "allowedValues": [1,3],
        "metadata": {
            "description": "Number of OpenShift infra nodes."                                                            }
    },
    "nodeInstanceCount": {
        "type": "int",
        "defaultValue": 2,
        "minValue": 1,
        "allowedValues": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,  20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30],
        "metadata": {                                                       
            "description": "Number of OpenShift nodes"
        }                                                                                       
    }
                                                                                                                    ```
