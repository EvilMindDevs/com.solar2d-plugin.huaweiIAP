//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.huaweiIAP;

import android.content.Context;
import android.content.IntentSender;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.config.LazyInputStream;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.IsSandboxActivatedReq;
import com.huawei.hms.iap.entity.IsSandboxActivatedResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.iap.entity.StartIapActivityReq;
import com.huawei.hms.iap.entity.StartIapActivityResult;
import com.huawei.hms.support.api.client.Status;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.LuaType;
import com.naef.jnlua.NamedJavaFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("WeakerAccess")
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
    private static int fListener;

    private static final String TAG = "Huawei In-App Purchases";
    private static final String EVENT_NAME = "Huawei In-App Purchases";

    private static final String SIGN_ALGORITHMS = "SHA256WithRSA";

    private static final String isEnvReady = "isEnvReady";
    private static final String isSandboxActivated = "isSandboxActivated";
    private static final String obtainOwnedPurchases = "obtainOwnedPurchases";
    private static final String verifySignature = "verifySignature";
    private static final String obtainProductInfo = "obtainProductInfo";
    private static final String createPurchaseIntent = "createPurchaseIntent";
    private static final String consumeOwnedPurchases = "consumeOwnedPurchases";
    private static final String obtainOwnedPurchaseRecord = "obtainOwnedPurchaseRecord";
    private static final String startIapActivity = "startIapActivity";

    @SuppressWarnings("unused")
    public LuaLoader() {
        fListener = CoronaLua.REFNIL;
        CoronaEnvironment.addRuntimeListener(this);
    }

    @Override
    public int invoke(LuaState L) {
        NamedJavaFunction[] luaFunctions = new NamedJavaFunction[]{
                new init(),
                new isEnvReady(),
                new isSandboxActivated(),
                new obtainOwnedPurchases(),
                new obtainProductInfo(),
                new createPurchaseIntent(),
                new consumeOwnedPurchases(),
                new verifySignature(),
                new obtainOwnedPurchaseRecord(),
                new startIapActivity(),
        };
        String libName = L.toString(1);
        L.register(libName, luaFunctions);
        return 1;
    }

    @SuppressWarnings("unused")
    private class init implements NamedJavaFunction {
        @Override
        public String getName() {
            return "init";
        }

        @Override
        public int invoke(LuaState L) {
            int listenerIndex = 1;

            if (CoronaLua.isListener(L, listenerIndex, EVENT_NAME)) {
                fListener = CoronaLua.newRef(L, listenerIndex);
            }

            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            AGConnectServicesConfig config = AGConnectServicesConfig.fromContext(activity);
            config.overlayWith(new LazyInputStream(activity) {
                public InputStream get(Context context) {
                    try {
                        Log.i(TAG, "agconnect-services.json was read");
                        return context.getAssets().open("agconnect-services.json");
                    } catch (IOException e) {
                        Log.i(TAG, "agconnect-services.json reading Exception " + e);
                        return null;
                    }
                }
            });

            return 0;
        }
    }

    @SuppressWarnings("unused")
    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider, final JSONArray data) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                L.pushString(data.toString());
                L.setField(-2, "data");
                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }

            }
        });
    }

    @SuppressWarnings("unused")
    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider, final JSONObject data) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                L.pushString(data.toString());
                L.setField(-2, "data");
                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }

            }
        });
    }

    @SuppressWarnings("unused")
    public static void dispatchEvent(final Boolean isError, final String message, final String type, final String provider) {
        CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
            @Override
            public void executeUsing(CoronaRuntime runtime) {
                LuaState L = runtime.getLuaState();

                CoronaLua.newEvent(L, EVENT_NAME);

                L.pushString(message);
                L.setField(-2, "message");

                L.pushBoolean(isError);
                L.setField(-2, "isError");

                L.pushString(type);
                L.setField(-2, "type");

                L.pushString(provider);
                L.setField(-2, "provider");

                try {
                    CoronaLua.dispatchEvent(L, fListener, 0);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public class isEnvReady implements NamedJavaFunction {

        @Override
        public String getName() {
            return isEnvReady;
        }

        @Override
        public int invoke(LuaState L) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
                    if (activity == null) {
                        return;
                    }
                    Task<IsEnvReadyResult> task = Iap.getIapClient(activity).isEnvReady();
                    task.addOnSuccessListener(new OnSuccessListener<IsEnvReadyResult>() {
                        @Override
                        public void onSuccess(IsEnvReadyResult result) {
                            // Obtain the result of the API request.
                            dispatchEvent(false, "IsEnvReadyResult Success", isEnvReady, TAG);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            if (e instanceof IapApiException) {
                                IapApiException apiException = (IapApiException) e;
                                Status status = apiException.getStatus();
                                if (status.getStatusCode() == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                                    // The HUAWEI ID has not signed in.
                                    dispatchEvent(true, "The HUAWEI ID has not signed in",
                                            isEnvReady, TAG);
                                } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                                    // The service area of the signed-in HUAWEI ID is not a country or region supported by IAP.
                                    dispatchEvent(true, "The service area of the signed-in HUAWEI ID is not a country or region supported by IAP.",
                                            isEnvReady, TAG);
                                } else {
                                    // Other external errors.
                                    dispatchEvent(true, "Other external errors.",
                                            isEnvReady, TAG);
                                }
                            }
                        }
                    });

                }
            });
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class isSandboxActivated implements NamedJavaFunction {

        @Override
        public String getName() {
            return isSandboxActivated;
        }

        @Override
        public int invoke(LuaState L) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }
            // Obtain the Activity object that calls the API.
            IapClient mClient = Iap.getIapClient(activity);
            Task<IsSandboxActivatedResult> task = mClient.isSandboxActivated(new IsSandboxActivatedReq());
            task.addOnSuccessListener(new OnSuccessListener<IsSandboxActivatedResult>() {
                @Override
                public void onSuccess(IsSandboxActivatedResult result) {
                    dispatchEvent(false, "isSandboxActivated Success", isSandboxActivated, TAG);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        int returnCode = apiException.getStatusCode();
                        dispatchEvent(true, "isSandboxActivated Failure, Error Code " + returnCode, isSandboxActivated, TAG);
                    } else {
                        dispatchEvent(true, "isSandboxActivated Failure, Other external errors", isSandboxActivated, TAG);
                    }
                }
            });

            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class obtainOwnedPurchases implements NamedJavaFunction {

        @Override
        public String getName() {
            return obtainOwnedPurchases;
        }

        @Override
        public int invoke(LuaState luaState) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            int productType;

            if (luaState.type(1) == LuaType.NUMBER) {
                productType = luaState.toInteger(1);
            } else {
                dispatchEvent(true, "obtainOwnedPurchases (integer) expected, got " + luaState.typeName(1),
                        obtainOwnedPurchases, TAG);
                return 0;
            }

            // Construct an OwnedPurchasesReq object.
            OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
            // priceType: 0: consumable; 1: non-consumable; 2: subscription
            ownedPurchasesReq.setPriceType(productType);
            // subs isValid. in result,
            // Call the obtainOwnedPurchases API to obtain all consumables that have been purchased but not delivered.
            Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
            task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
                @Override
                public void onSuccess(OwnedPurchasesResult result) {
                    // Obtain the execution result.
                    if (result == null || result.getInAppPurchaseDataList() == null || result.getInAppPurchaseDataList().isEmpty()) {
                        dispatchEvent(false, "There is not product", obtainOwnedPurchases, TAG);
                        return;
                    }

                    List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
                    List<String> inAppSignature = result.getInAppSignature();
                    JSONArray ownedProducts = new JSONArray();
                    for (int i = 0; i < inAppPurchaseDataList.size(); i++) {
                        JSONObject ownedProduct = new JSONObject();
                        try {
                            ownedProduct.put("inAppPurchaseData", inAppPurchaseDataList.get(i));
                            ownedProduct.put("inAppPurchaseDataSignature", inAppSignature.get(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        ownedProducts.put(ownedProduct);
                    }
                    dispatchEvent(false, "obtainOwnedPurchases products", obtainOwnedPurchases, TAG, ownedProducts);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        Status status = apiException.getStatus();
                        int returnCode = apiException.getStatusCode();
                        dispatchEvent(true, "Error Code " + returnCode + " / " + apiException.getMessage(), obtainOwnedPurchases, TAG);

                    } else {
                        // Other external errors.
                        dispatchEvent(true, "Other external error", obtainOwnedPurchases, TAG);
                    }
                }
            });
            return 0;
        }
    }

    private ProductInfoReq createProductInfoReq(int productType, ArrayList<String> productIds) {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(productType);
        req.setProductIds(productIds);
        return req;
    }

    private JSONArray productInfoToJson(List<ProductInfo> list) {
        JSONArray array = new JSONArray();
        for (ProductInfo info : list) {
            JSONObject product = new JSONObject();
            try {
                product.put("productId", info.getProductId());
                product.put("priceType", info.getPriceType());
                product.put("price", info.getPrice());
                product.put("microsPrice", info.getMicrosPrice());
                product.put("originalLocalPrice", info.getOriginalLocalPrice());
                product.put("originalMicroPrice", info.getOriginalMicroPrice());
                product.put("currency", info.getCurrency());
                product.put("productName", info.getProductName());
                product.put("productDesc", info.getProductDesc());
                product.put("subSpecialPriceMicros", info.getSubSpecialPriceMicros());
                product.put("subSpecialPeriodCycles", info.getSubSpecialPeriodCycles());
                product.put("subProductLevel", info.getSubProductLevel());
                product.put("status", info.getStatus());
                product.put("subFreeTrialPeriod", info.getSubFreeTrialPeriod());
                product.put("subGroupId", info.getSubGroupId());
                product.put("subGroupTitle", info.getSubGroupTitle());
                product.put("subSpecialPeriod", info.getSubSpecialPeriod());
                product.put("subPeriod", info.getSubPeriod());
                product.put("subSpecialPrice", info.getSubSpecialPeriod());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
            array.put(product);
        }
        return array;
    }

    @SuppressWarnings("unused")
    public class obtainProductInfo implements NamedJavaFunction {

        @Override
        public String getName() {
            return obtainProductInfo;
        }

        @Override
        public int invoke(LuaState luaState) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            int productType;
            ArrayList<String> productList = new ArrayList<>();

            if (luaState.type(1) == LuaType.NUMBER) {
                productType = luaState.toInteger(1);
            } else {
                dispatchEvent(true, "productType (integer) expected, got " + luaState.typeName(1), obtainProductInfo, TAG);
                return 0;
            }

            if (luaState.type(2) == LuaType.TABLE) {
                // build supported ad types
                int ntypes = luaState.length(-1);

                if (ntypes > 0) {
                    for (int i = 1; i <= ntypes; i++) {
                        // push array value onto stack
                        luaState.rawGet(-1, i);
                        // add keyword to array
                        if (luaState.type(-1) == LuaType.STRING) {
                            productList.add(luaState.toString(-1));
                        } else {
                            dispatchEvent(true, "productId[" + i + "] (string) expected, got: " + luaState.typeName(-1), obtainProductInfo, TAG);
                            return 0;
                        }
                        luaState.pop(1);
                    }
                } else {
                    dispatchEvent(true, "productId table cannot be empty", obtainProductInfo, TAG);
                    return 0;
                }
            } else {
                dispatchEvent(true, "productId (table) expected, got: " + luaState.typeName(-1), obtainProductInfo, TAG);
                return 0;
            }

            IapClient iapClient = Iap.getIapClient(activity);
            Task<ProductInfoResult> task = iapClient.obtainProductInfo(createProductInfoReq(productType, productList));

            task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
                @Override
                public void onSuccess(ProductInfoResult result) {
                    if (result != null && !result.getProductInfoList().isEmpty()) {
                        List<ProductInfo> list = result.getProductInfoList();
                        dispatchEvent(false, "There are " + list.size() + " products", obtainProductInfo, TAG, productInfoToJson(list));
                    } else {
                        dispatchEvent(true, "There is no product", obtainProductInfo, TAG);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    dispatchEvent(true, "obtainProductInfo error " + e.getMessage(), obtainProductInfo, TAG);
                }
            });
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class createPurchaseIntent implements NamedJavaFunction {

        @Override
        public String getName() {
            return createPurchaseIntent;
        }

        @Override
        public int invoke(final LuaState luaState) {
            final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            final int productType;
            final String productId;
            String payload = "";

            if (luaState.type(1) == LuaType.NUMBER) {
                productType = luaState.toInteger(1);
            } else {
                dispatchEvent(true, "productType (integer (0|1|2)) expected, got " + luaState.typeName(1), createPurchaseIntent, TAG);
                return 0;
            }

            if (luaState.type(2) == LuaType.STRING) {
                productId = luaState.toString(2);
            } else {
                dispatchEvent(true, "productId (String) expected, got " + luaState.typeName(1), createPurchaseIntent, TAG);
                return 0;
            }

            if (luaState.type(3) == LuaType.STRING) {
                payload = luaState.toString(1);
            }

            final int requestCode = activity.registerActivityResultHandler(new CoronaActivity.OnActivityResultHandler() {
                @Override
                public void onHandleActivityResult(CoronaActivity activity, int requestCode, int resultCode, android.content.Intent data) {
                    activity.unregisterActivityResultHandler(this);
                    if (data == null) {
                        dispatchEvent(true, "Purchase Intent Data is null", createPurchaseIntent, TAG);
                        return;
                    }
                    // Call the parsePurchaseResultInfoFromIntent method to parse the payment result.
                    PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(activity).parsePurchaseResultInfoFromIntent(data);
                    switch (purchaseResultInfo.getReturnCode()) {
                        case OrderStatusCode.ORDER_STATE_CANCEL:
                            dispatchEvent(true, "Order Cancelled", createPurchaseIntent, TAG);
                            // The user cancels the purchase.
                            break;
                        case OrderStatusCode.ORDER_STATE_FAILED:
                            Log.i(TAG, "ORDER_STATE_FAILED");
                            dispatchEvent(true, "Order Failed", createPurchaseIntent, TAG);
                            break;
                        case OrderStatusCode.ORDER_PRODUCT_OWNED:
                            Log.i(TAG, "ORDER_PRODUCT_OWNED");
                            dispatchEvent(true, "Order Product Owned", createPurchaseIntent, TAG);

                            // Check whether the delivery is successful.
                            break;
                        case OrderStatusCode.ORDER_STATE_SUCCESS:
                            // The payment is successful.
                            Log.i(TAG, "ORDER_STATE_SUCCESS");

                            String inAppPurchaseData = purchaseResultInfo.getInAppPurchaseData();
                            String inAppPurchaseDataSignature = purchaseResultInfo.getInAppDataSignature();
                            JSONObject product = new JSONObject();
                            try {
                                product.put("inAppPurchaseData", inAppPurchaseData);
                                product.put("inAppPurchaseDataSignature", inAppPurchaseDataSignature);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dispatchEvent(false, "Order Success", createPurchaseIntent, TAG, product);
                            break;
                        default:
                            break;
                    }
                }
            });

            PurchaseIntentReq req = new PurchaseIntentReq();
            // Only those products already configured in AppGallery Connect can be purchased through the createPurchaseIntent API.
            req.setProductId(productId);
            // priceType: 0: consumable; 1: non-consumable; 2: subscription
            req.setPriceType(productType);
            req.setDeveloperPayload(payload);
            // Call the createPurchaseIntent API to create a PMS product order.
            Task<PurchaseIntentResult> task = Iap.getIapClient(activity).createPurchaseIntent(req);
            task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
                @Override
                public void onSuccess(PurchaseIntentResult result) {
                    // Obtain the order creation result.

                    Status status = result.getStatus();
                    if (status.hasResolution()) {
                        try {
                            status.startResolutionForResult(activity, requestCode);
                        } catch (IntentSender.SendIntentException exp) {
                            dispatchEvent(true, "Purchase Intent => " + exp.getMessage(), createPurchaseIntent, TAG);
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        Status status = apiException.getStatus();
                        int returnCode = apiException.getStatusCode();
                        dispatchEvent(true, "Purchase Intent => Error Code " + returnCode, createPurchaseIntent, TAG);
                    } else {
                        dispatchEvent(true, "Purchase Intent => Other external errors", createPurchaseIntent, TAG);
                    }
                }
            });
            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class consumeOwnedPurchases implements NamedJavaFunction {

        @Override
        public String getName() {
            return consumeOwnedPurchases;
        }

        @Override
        public int invoke(LuaState luaState) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            final String inAppPurchaseData;

            if (luaState.type(1) == LuaType.STRING) {
                inAppPurchaseData = luaState.toString(1);
            } else {
                dispatchEvent(true, "inAppPurchaseData (String) expected, got " + luaState.typeName(1), consumeOwnedPurchases, TAG);
                return 0;
            }

            // Construct a ConsumeOwnedPurchaseReq object.
            ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
            String purchaseToken = "";
            try {
                // Obtain the value of purchaseToken from inAppPurchaseData. You can obtain inAppPurchaseData from a payment request or by calling the obtainOwnedPurchases API.
                //InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                purchaseToken = inAppPurchaseDataBean.getPurchaseToken();
            } catch (JSONException e) {
            }
            req.setPurchaseToken(purchaseToken);

            // Call the consumeOwnedPurchase API.
            Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(activity).consumeOwnedPurchase(req);
            task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
                @Override
                public void onSuccess(ConsumeOwnedPurchaseResult result) {
                    JSONObject product = new JSONObject();
                    try {
                        product.put("inAppPurchaseData", result.getConsumePurchaseData());
                        product.put("inAppPurchaseDataSignature", result.getDataSignature());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    dispatchEvent(false, "consumeOwnedPurchases() success", consumeOwnedPurchases, TAG, product);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        Status status = apiException.getStatus();
                        int returnCode = apiException.getStatusCode();
                        dispatchEvent(true, "consumeOwnedPurchases failled, Error Code => " + returnCode, consumeOwnedPurchases, TAG);
                    } else {
                        dispatchEvent(true, "consumeOwnedPurchases failled", consumeOwnedPurchases, TAG);
                    }
                }
            });

            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class verifySignature implements NamedJavaFunction {

        @Override
        public String getName() {
            return verifySignature;
        }

        @Override
        public int invoke(LuaState luaState) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            final String inAppPurchaseData;
            final String inAppPurchaseDataSignature;
            final String publicKey;

            if (luaState.type(1) == LuaType.STRING) {
                inAppPurchaseData = luaState.toString(1);
            } else {
                dispatchEvent(true, "verifySignature (String inAppPurchaseData, String inAppPurchaseDataSignature, String publicKey) expected, got "
                        + luaState.typeName(1) + " / " + luaState.typeName(2) + " / " + luaState.typeName(3), verifySignature, TAG);
                return 0;
            }

            if (luaState.type(2) == LuaType.STRING) {
                inAppPurchaseDataSignature = luaState.toString(2);
            } else {
                dispatchEvent(true, "verifySignature (String inAppPurchaseData, String inAppPurchaseDataSignature, String publicKey) expected, got "
                        + luaState.typeName(1) + " / " + luaState.typeName(2) + " / " + luaState.typeName(3), verifySignature, TAG);
                return 0;
            }

            if (luaState.type(3) == LuaType.STRING) {
                publicKey = luaState.toString(3);
            } else {
                dispatchEvent(true, "verifySignature (String inAppPurchaseData, String inAppPurchaseDataSignature, String publicKey) expected, got "
                        + luaState.typeName(1) + " / " + luaState.typeName(2) + " / " + luaState.typeName(3), verifySignature, TAG);
                return 0;
            }

            luaState.pushBoolean(doCheck(inAppPurchaseData, inAppPurchaseDataSignature, publicKey));
            return 1;
        }
    }

    @SuppressWarnings("unused")
    public class obtainOwnedPurchaseRecord implements NamedJavaFunction {

        @Override
        public String getName() {
            return obtainOwnedPurchaseRecord;
        }

        @Override
        public int invoke(LuaState luaState) {
            CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            final int ProductType;

            if (luaState.type(1) == LuaType.NUMBER) {
                ProductType = luaState.toInteger(1);
            } else {
                dispatchEvent(true, "obtainOwnedPurchaseRecord (String ProductType) expected, got " + luaState.typeName(1), obtainOwnedPurchaseRecord, TAG);
                return 0;
            }

            // Construct an OwnedPurchasesReq object.
            OwnedPurchasesReq req = new OwnedPurchasesReq();

            // priceType: 0: consumable; 1: non-consumable; 2: subscription
            req.setPriceType(ProductType);

            // Call the obtainOwnedPurchaseRecord API.
            Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchaseRecord(req);
            task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
                @Override
                public void onSuccess(OwnedPurchasesResult result) {
                    // Obtain the result returned upon a successful API call.
                    // Obtain the execution result.
                    if (result == null || result.getInAppPurchaseDataList() == null || result.getInAppPurchaseDataList().isEmpty()) {
                        dispatchEvent(false, "There is not product", obtainOwnedPurchaseRecord, TAG);
                        return;
                    }

                    List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
                    List<String> inAppSignature = result.getInAppSignature();
                    JSONArray ownedProducts = new JSONArray();
                    for (int i = 0; i < inAppPurchaseDataList.size(); i++) {
                        JSONObject ownedProduct = new JSONObject();
                        try {
                            ownedProduct.put("inAppPurchaseData", inAppPurchaseDataList.get(i));
                            ownedProduct.put("inAppPurchaseDataSignature", inAppSignature.get(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        ownedProducts.put(ownedProduct);
                    }
                    dispatchEvent(false, "obtainOwnedPurchaseRecord products", obtainOwnedPurchaseRecord, TAG, ownedProducts);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        int returnCode = apiException.getStatusCode();
                        dispatchEvent(true, "obtainOwnedPurchaseRecord Failed Error Code " + returnCode, obtainOwnedPurchaseRecord, TAG);
                    } else {
                        dispatchEvent(true, "obtainOwnedPurchaseRecord Failed", obtainOwnedPurchaseRecord, TAG);
                    }
                }
            });

            return 0;
        }
    }

    @SuppressWarnings("unused")
    public class startIapActivity implements NamedJavaFunction {

        @Override
        public String getName() {
            return startIapActivity;
        }

        @Override
        public int invoke(LuaState luaState) {
            final CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
            if (activity == null) {
                return 0;
            }

            //TYPE_SUBSCRIBE_MANAGER_ACTIVITY = 2;
            //TYPE_SUBSCRIBE_EDIT_ACTIVITY = 3;

            int Type;
            String productId = "";

            if (luaState.type(1) == LuaType.NUMBER) {
                Type = luaState.toInteger(1);
            } else {
                dispatchEvent(true, "startIapActivity (int Type) expected, got " + luaState.typeName(1), startIapActivity, TAG);
                return 0;
            }

            if (Type == 3) {
                if (luaState.type(2) == LuaType.STRING) {
                    productId = luaState.toString(2);
                } else {
                    dispatchEvent(true, "startIapActivity (int Type, String productId) expected, got " + luaState.typeName(1) +
                            "/" + luaState.typeName(2), startIapActivity, TAG);
                    return 0;
                }
            }

            StartIapActivityReq req = new StartIapActivityReq();
            req.setType(Type);
            if (Type == 3) {
                req.setSubscribeProductId(productId);
            }

            // Obtain the Activity object that calls the API.
            IapClient mClient = Iap.getIapClient(activity);
            Task<StartIapActivityResult> task = mClient.startIapActivity(req);
            task.addOnSuccessListener(new OnSuccessListener<StartIapActivityResult>() {
                @Override
                public void onSuccess(StartIapActivityResult result) {
                    Log.i("startIapActivity", "onSuccess");
                    if (result != null) {
                        result.startActivity(activity);
                        dispatchEvent(false, "startIapActivity Success, result is not null", startIapActivity, TAG);
                    } else {
                        dispatchEvent(true, "startIapActivity Fail,  result is null", startIapActivity, TAG);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    dispatchEvent(true, "startIapActivity Fail" + e.getMessage(), startIapActivity, TAG);
                }
            });

            return 0;
        }
    }

    private boolean doCheck(String content, String sign, String publicKey) {
        if (TextUtils.isEmpty(publicKey)) {
            Log.i(TAG, "publicKey is null");
            return false;
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(publicKey, Base64.DEFAULT);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

            signature.initVerify(pubKey);
            signature.update(content.getBytes("utf-8"));

            boolean bverify = signature.verify(Base64.decode(sign, Base64.DEFAULT));
            return bverify;

        } catch (NoSuchAlgorithmException e) {
            Log.i(TAG, "doCheck NoSuchAlgorithmException" + e);
        } catch (InvalidKeySpecException e) {
            Log.i(TAG, "doCheck InvalidKeySpecException" + e.getMessage());
        } catch (InvalidKeyException e) {
            Log.i(TAG, "doCheck InvalidKeyException" + e);
        } catch (SignatureException e) {
            Log.i(TAG, "doCheck SignatureException" + e);
        } catch (UnsupportedEncodingException e) {
            Log.i(TAG, "doCheck UnsupportedEncodingException" + e);
        }
        return false;
    }

    @Override
    public void onLoaded(CoronaRuntime runtime) {
    }

    @Override
    public void onStarted(CoronaRuntime runtime) {
    }

    @Override
    public void onSuspended(CoronaRuntime runtime) {
    }

    @Override
    public void onResumed(CoronaRuntime runtime) {
    }

    @Override
    public void onExiting(CoronaRuntime runtime) {
        CoronaLua.deleteRef(runtime.getLuaState(), fListener);
        fListener = CoronaLua.REFNIL;
    }

}
