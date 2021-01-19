# Huawei In-App Purchases Solar2d Plugin

This plugin was created based on Huawei IAP. Please [check](https://developer.huawei.com/consumer/en/hms/huawei-iap/) for detailed information about Huawei IAP. 

***For apps using the HMS SDK for in-app payment, the package name must end with ".HUAWEI" or ".huawei" (case sensitive).***

In order to use the Huawei IAP, you must first create an account from developer.huawei.com. And after logging in with your account, you must create a project in the huawei console in order to use HMS kits.

## Project Setup

To use the plugin please add following to `build.settings`

```lua
{
    plugins = {
        ["plugin.huaweiIAP"] = {
            publisherId = "com.solar2d",
        },
    },
}
```

And then you have to create keystore for your app. And you must generate sha-256 bit fingerprint from this keystore using the command here. You have to define this fingerprint to your project on the huawei console.

And you must add the keystore you created while building your project. 
Also you need to give the package-name of the project you created on Huawei Console.
And you need to put `agconnect-services.json` file into `main.lua` directory.

After all the configuration processes, you must define the plugin in main.lua.

```lua
local huaweiIAP = require "plugin.huaweiIAP"

local function listener(event)
    print(event) -- (table)
end

huaweiIAP.init(listener) -- sets listener and inits plugin
```

We should call all methods through huaweiIAP object. And you can take result informations from listener.

## Methods in the Plugin

### isEnvReady
Checks whether the currently signed-in HUAWEI ID is located in a country or region where HUAWEI IAP is available.

```lua
	huaweiIAP.isEnvReady()

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = isEnvReady (text)
		provider = Huawei IAP
	}--]]
```

### isSandboxActivated
Checks whether the signed-in HUAWEI ID and the app APK version meet the requirements of the sandbox testing.

```lua
	huaweiIAP.isSandboxActivated()

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = isSandboxActivated (text)
		provider = Huawei IAP
	}--]]
```

### obtainOwnedPurchases
Queries information about all purchased products, including consumables, non-consumables, and subscriptions.

```lua
	-- 0 => consumables, 1 => non-consumables, 2 => subscriptions
	huaweiIAP.obtainOwnedPurchases(0) 

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = obtainOwnedPurchases (text)
		provider = Huawei IAP (text)
		data = [
			{inAppPurchaseData : text, inAppPurchaseDataSignature:text},
			....
			]
	}]]--
```


### obtainProductInfo
Obtains product details configured in AppGallery Connect. If you use Huawei's PMS to price products, you can use this API to obtain product details from the PMS to ensure that the product information in your app is the same as that displayed on the checkout page of HUAWEI IAP.

```lua
	-- 0 => consumables, 1 => non-consumables, 2 => subscriptions
	huaweiIAP.obtainProductInfo(0 , {"productId","productId"})

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = obtainProductInfo (text)
		provider = Huawei IAP (text) 
		data = [
			{productId : text, priceType:text, price:text, productName:text, productDesc:text, ...},
			....
			]
	}]]--
```


### createPurchaseIntent
Creates orders for products managed by the PMS, including consumables, non-consumables, and subscriptions.

```lua
	-- 0 => consumables, 1 => non-consumables, 2 => subscriptions
	huaweiIAP.createPurchaseIntent(0 , "productId")

    --Result 
    --[[Table {
		isError = true|false
		message = text
		type = createPurchaseIntent (text) 
		provider = Huawei IAP (text) 
		data = {
			inAppPurchaseData : text, 
			inAppPurchaseDataSignature:text
			}
	}]]--
```


### consumeOwnedPurchases
Consumes a consumable after the consumable is delivered to a user who has completed payment.

```lua
	--huaweiIAP.consumeOwnedPurchases(inAppPurchaseData)

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = consumeOwnedPurchases (text) 
		provider = Huawei IAP (text) 
		data = {
			inAppPurchaseData : text, 
			inAppPurchaseDataSignature:text
		}]]--
```


### verifySignature
Verificaiton operation is performed using the signature and product information and public key of the purchased product.

```lua
    if huaweiIAP.verifySignature(inAppPurchaseData, inAppPurchaseDataSignature, publicKey) then
        ...
    else
        print("Signature Verification is failed.")
    end
	  
	--Return (true|false)
```

### obtainOwnedPurchaseRecord
Obtains the consumption history of a consumable or all subscription receipts of a subscription.

```lua
	-- 0 => consumables, 1 => non-consumables, 2 => subscriptions
	huaweiIAP.obtainOwnedPurchaseRecord(2)

	--Result 
	--[[Table {
		isError = true|false
		message = text
		type = obtainOwnedPurchaseRecord (text) 
		provider = Huawei IAP (text) 
		data = [
			{inAppPurchaseData : text, inAppPurchaseDataSignature:text},
			....
			]
	}]]--
```

### startIapActivity
Displays pages of HUAWEI IAP, including:
	- Subscription editing page
	- Subscription management page

```lua
	--TYPE_SUBSCRIBE_MANAGER_ACTIVITY = 2;
   	huaweiIAP.startIapActivity(2)

 	--TYPE_SUBSCRIBE_EDIT_ACTIVITY = 3;
    huaweiIAP.startIapActivity(3, "productId")

    --Result 
    --[[Table {
		isError = true|false
		message = text
		type = startIapActivity (text) 
		provider = Huawei IAP (text) 
	}]]--
```

## Requirement
Java SDK 1.7 or later
HMS Core (APK) 3.0.0.300 or later
HMS Core SDK 4.0.0.300 or later

## References
Constant Values https://developer.huawei.com/consumer/en/doc/development/HMSCore-References-V5/constant-values-0000001050165190-V5
IAP Result Codes https://developer.huawei.com/consumer/en/doc/development/HMSCore-References-V5/client-error-code-0000001050746111-V5

## License
MIT

