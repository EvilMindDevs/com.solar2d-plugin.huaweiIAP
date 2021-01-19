local Library = require "CoronaLibrary"

local lib = Library:new{ name='plugin.huaweiIAP', publisherId='com.solar2d' }

local placeholder = function()
	print( "WARNING: The '" .. lib.name .. "' library is not available on this platform." )
end


lib.init = placeholder
lib.isEnvReady = placeholder
lib.isSandboxActivated = placeholder
lib.obtainOwnedPurchases = placeholder
lib.obtainProductInfo = placeholder
lib.createPurchaseIntent = placeholder
lib.consumeOwnedPurchases = placeholder
lib.verifySignature = placeholder
lib.obtainOwnedPurchaseRecord = placeholder
lib.startIapActivity = placeholder

-- Return an instance
return lib