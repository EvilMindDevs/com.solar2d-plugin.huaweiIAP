local huaweiIAP = require "plugin.huaweiIAP"
local widget = require( "widget" )
local json = require("json")

local publicKey = "XXXXXXXXX"

-- priceType: 0: consumable; 1: non-consumable; 2: subscription
local consumableProductList = {}
local nonconsumableProductList = {}
local subscriptionProductList = {}

local obtainOwnedPurchases = {}

local TAG = "Huawei IAP"

local isEnvReady = false
local isSandboxActivated = false

local displayName = display.newText( "Huawei In-App Purchases", display.contentCenterX, 60, native.systemFont, 10 )
displayName:setFillColor( 255, 255, 255 )

local function listener( event )

    if event.type == "isEnvReady" then
        if not event.isError then 
            isEnvReady = true
        else 
            isEnvReady = false
        end

    elseif event.type == "isSandboxActivated" then
        if not event.isError then 
            isSandboxActivated = true
        else 
            isSandboxActivated = false
        end

    elseif event.type == "obtainOwnedPurchases" then
        if not event.isError then 
            obtainOwnedPurchases = json.decode( event.data )
            for key,value in pairs(obtainOwnedPurchases) do --actualcode
                print(TAG, "inAppPurchaseDataSignature ", value.inAppPurchaseDataSignature, " / inAppPurchaseData =>  ", value.inAppPurchaseData)
                if huaweiIAP.verifySignature(value.inAppPurchaseData, value.inAppPurchaseDataSignature, publicKey) then
                    print(TAG, "Signature Verification is success.")
                    huaweiIAP.consumeOwnedPurchases(value.inAppPurchaseData)
                else
                    print(TAG, "Signature Verification is failed.")
                end
                
            end
        else 
            print(TAG, "obtainOwnedPurchases Error => ", event.message)
        end

    elseif event.type == "obtainProductInfo" then
        if not event.isError then
            consumableProductList = json.decode( event.data )
            for key,value in ipairs(consumableProductList) do
               print(TAG, ", ", value.productName, " / ", value.productId, " / ", value.priceType)
            end
        else
            print(TAG, "obtainProductInfo Error => ",event.message)
        end

    elseif event.type == "createPurchaseIntent" then
        if not event.isError then
            print(TAG, ", ", event.message, " = ", json.decode(event.data).inAppPurchaseData)
            print(TAG, ", ", event.message, " = ", json.decode(event.data).inAppPurchaseDataSignature)
        else
            print(TAG, "createPurchaseIntent Error => ",event.message)
        end

    elseif event.type == "consumeOwnedPurchases" then
        if not event.isError then 
            print(TAG, " consumeOwnedPurchases =>", event.message)
        else 
            print(TAG, " consumeOwnedPurchases Error =>", event.message)
        end

    elseif event.type == "obtainOwnedPurchaseRecord" then
        if not event.isError then 
            print(TAG, "obtainOwnedPurchaseRecord =>", event.message)
        else 
            print(TAG, "obtainOwnedPurchaseRecord Error =>", event.message)
        end

    else
        print(event.message)
    end

end   

huaweiIAP.init( listener )

-- IAP Kit
local isEnvReady = widget.newButton(
    {
        left = 65,
        top = 110,
        id = "isEnvReady",
        label = "isEnvReady",
        onPress = huaweiIAP.isEnvReady,
        width = 190,
        height = 30
    }
)

local isSandboxActivated = widget.newButton(
    {
        left = 65,
        top = 140,
        id = "isSandboxActivated",
        label = "isSandboxActivated",
        onPress = huaweiIAP.isSandboxActivated,
        width = 190,
        height = 30
    }
)

local obtainOwnedPurchases = widget.newButton(
    {
        left = 65,
        top = 170,
        id = "obtainOwnedPurchases",
        label = "obtainOwnedPurchases",
        onPress = function()
            huaweiIAP.obtainOwnedPurchases(0)
        end,
        width = 190,
        height = 30
    }
)

local obtainProductInfo = widget.newButton(
    {
        left = 65,
        top = 200,
        id = "obtainProductInfo",
        label = "obtainProductInfo",
        onPress = function ()
            huaweiIAP.obtainProductInfo(0 , {"consumableProductId","consumableProductId"})
            -- huaweiIAP.obtainProductInfo(1 , {"nonconsumableProductId"})
            -- huaweiIAP.obtainProductInfo(2 , {"subscriptionProductId"})
        end,
        width = 190,
        height = 30
    }
)

local createPurchaseIntent = widget.newButton(
    {
        left = 65,
        top = 230,
        id = "createPurchaseIntent",
        label = "createPurchaseIntent",
        onPress = function()
            huaweiIAP.createPurchaseIntent(0 , "consumableProductId")
            -- huaweiIAP.createPurchaseIntent(1 , "nonconsumableProductId")
            -- huaweiIAP.createPurchaseIntent(2 , "subscriptionProductId")
        end,
        width = 190,
        height = 30
    }
)



local obtainOwnedPurchaseRecord = widget.newButton(
    {
        left = 65,
        top = 260,
        id = "obtainOwnedPurchaseRecord",
        label = "obtainOwnedPurchaseRecord",
        onPress = function ()
            huaweiIAP.obtainOwnedPurchaseRecord(2)
        end,
        width = 190,
        height = 30
    }
)


local startIapActivity = widget.newButton(
    {
        left = 65,
        top = 290,
        id = "startIapActivity",
        label = "startIapActivity",
        onPress = function()
            -- huaweiIAP.startIapActivity(2)
            huaweiIAP.startIapActivity(3, "subscriptionProductId")
        end,
        width = 190,
        height = 30
    }
)
