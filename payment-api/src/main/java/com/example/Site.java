package com.example;

import spark.*;
import spark.servlet.SparkApplication;
import java.util.HashMap;
import com.twocheckout.*;
import com.twocheckout.model.*;


public class Site implements SparkApplication {

    @Override
    public void init() {

        Spark.get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                String content =
                        "<!DOCTYPE html>\n" +
                                "<html>\n" +
                                "    <head>\n" +
                                "        <title>Python Example</title>\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <form id=\"myCCForm\" action=\"/payment-api/order\" method=\"post\">\n" +
                                "            <input id=\"token\" name=\"token\" type=\"hidden\" value=\"\">\n" +
                                "            <div>\n" +
                                "                <label>\n" +
                                "                    <span>Card Number</span>\n" +
                                "                </label>\n" +
                                "                <input id=\"ccNo\" type=\"text\" size=\"20\" value=\"\" autocomplete=\"off\" required />\n" +
                                "            </div>\n" +
                                "            <div>\n" +
                                "                <label>\n" +
                                "                    <span>Expiration Date (MM/YYYY)</span>\n" +
                                "                </label>\n" +
                                "                <input type=\"text\" size=\"2\" id=\"expMonth\" required />\n" +
                                "                <span> / </span>\n" +
                                "                <input type=\"text\" size=\"2\" id=\"expYear\" required />\n" +
                                "            </div>\n" +
                                "            <div>\n" +
                                "                <label>\n" +
                                "                    <span>CVC</span>\n" +
                                "                </label>\n" +
                                "                <input id=\"cvv\" size=\"4\" type=\"text\" value=\"\" autocomplete=\"off\" required />\n" +
                                "            </div>\n" +
                                "            <input type=\"submit\" value=\"Submit Payment\">\n" +
                                "        </form>\n" +
                                "\n" +
                                "        <script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js\"></script>\n" +
                                "        <script type=\"text/javascript\" src=\"https://www.2checkout.com/checkout/api/2co.min.js\"></script>\n" +
                                "\n" +
                                "        <script>\n" +
                                "            // Called when token created successfully.\n" +
                                "            var successCallback = function(data) {\n" +
                                "                var myForm = document.getElementById('myCCForm');\n" +
                                "\n" +
                                "                // Set the token as the value for the token input\n" +
                                "                myForm.token.value = data.response.token.token;\n" +
                                "\n" +
                                "                // IMPORTANT: Here we call `submit()` on the form element directly instead of using jQuery to prevent and infinite token request loop.\n" +
                                "                myForm.submit();\n" +
                                "            };\n" +
                                "\n" +
                                "            // Called when token creation fails.\n" +
                                "            var errorCallback = function(data) {\n" +
                                "                if (data.errorCode === 200) {\n" +
                                "                    tokenRequest();\n" +
                                "                } else {\n" +
                                "                    alert(data.errorMsg);\n" +
                                "                }\n" +
                                "            };\n" +
                                "\n" +
                                "            var tokenRequest = function() {\n" +
                                "                // Setup token request arguments\n" +
                                "                var args = {\n" +
                                "                    sellerId: \"sandbox-seller-id\",\n" +
                                "                    publishableKey: \"sandbox-publishable-key\",\n" +
                                "                    ccNo: $(\"#ccNo\").val(),\n" +
                                "                    cvv: $(\"#cvv\").val(),\n" +
                                "                    expMonth: $(\"#expMonth\").val(),\n" +
                                "                    expYear: $(\"#expYear\").val()\n" +
                                "                };\n" +
                                "\n" +
                                "                // Make the token request\n" +
                                "                TCO.requestToken(successCallback, errorCallback, args);\n" +
                                "            };\n" +
                                "\n" +
                                "            $(function() {\n" +
                                "                // Pull in the public encryption key for our environment\n" +
                                "                TCO.loadPubKey('sandbox');\n" +
                                "\n" +
                                "                $(\"#myCCForm\").submit(function(e) {\n" +
                                "                    // Call our token request function\n" +
                                "                    tokenRequest();\n" +
                                "\n" +
                                "                    // Prevent form from submitting\n" +
                                "                    return false;\n" +
                                "                });\n" +
                                "            });\n" +
                                "        </script>\n" +
                                "    </body>\n" +
                                "</html>\n";
                response.type("text/html");
                return content;
            }
        });

        Spark.post(new Route("/order") {
            @Override
            public Object handle(Request request, Response response) {

                String message;

                Twocheckout.privatekey = "sandbox-private-key";
                Twocheckout.mode = "sandbox";

                try {
                    HashMap billing = new HashMap();
                    billing.put("name", "Testing Tester");
                    billing.put("addrLine1", "123 Test St");
                    billing.put("city", "Columbus");
                    billing.put("state", "OH");
                    billing.put("country", "USA");
                    billing.put("zipCode", "43230");
                    billing.put("email", "example@2co.com");
                    billing.put("phone", "555-555-5555");

                    HashMap params = new HashMap();
                    params.put("sellerId", "sandbox-seller-id");
                    params.put("merchantOrderId", "test123");
                    params.put("token", request.queryParams("token"));
                    params.put("currency", "USD");
                    params.put("total", "1.00");
                    params.put("billingAddr", billing);

                    Authorization result = TwocheckoutCharge.authorize(params);
                    message = result.getResponseMsg();
                } catch (TwocheckoutException e) {
                    message = e.getMessage();
                }
                return message;
            }
        });
    }
}
