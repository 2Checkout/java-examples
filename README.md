2Checkout Payment API Java Tutorial
=========================

In this tutorial we will walk through integrating the 2Checkout Payment API to securely tokenize and charge a credit card and using the [2Checkout Java library](https://www.2checkout.com/documentation/libraries/java). You will need a 2Checkout sandbox account to complete the tutorial so if you have not already, [signup for an account](https://sandbox.2checkout.com/sandbox/signup) and [generate your Payment API keys](https://www.2checkout.com/documentation/sandbox/payment-api-testing).

----

### Application Setup

For our example application, we will be using the spark framework and 2Checkout's Java library. The 2Checkout Java library provides us with a simple bindings to the API, INS and Checkout process so that we can integrate each feature with only a few lines of code. In this example, we will only be using the Payment API functionality of the library, but for an example of the other features you can view this tutorial: [https://github.com/2Checkout/2checkout-java-tutorial](https://github.com/2Checkout/2checkout-java-tutorial)

```
$ git clone https://github.com/2Checkout/2checkout-java.git
```

To pull in the dependencies, we will use [Gradle](http://www.gradle.org/).

To start off, create a directory called payment-api and a file inside named build.gradle with the following:

```
apply plugin: 'jetty'

repositories {
    mavenCentral()
}

dependencies {
    compile 'com.sparkjava:spark-core:1.1.1'
    compile 'org.apache.httpcomponents:httpclient:4.2.5'
    compile 'com.google.code.gson:gson:2.2.3'
    compile files('./2checkout-java.jar')
}
```

Grab the [2checkout-java.jar file](https://github.com/2Checkout/2checkout-java) from Github and drop it in your new 'payment-api' directory.

Now pull in the application dependencies by running the 'gradle' command.

```
$ gradle
```

Next create the directory structure by running the following commands in the 'payment-api' directory.

```
mkdir -p src/main/java/com/example/
mkdir -p src/main/java/webapp/WEB-INF/
```

Create a file named web.xml in the 'WEB-INF' directory with the following contents.

```
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
          http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
           version="2.5">

    <filter>
        <filter-name>SparkFilter</filter-name>
        <filter-class>spark.servlet.SparkFilter</filter-class>
        <init-param>
            <param-name>applicationClass</param-name>
            <param-value>com.example.Site</param-value>
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>SparkFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

</web-app>
```

And create a file named Site.java in the 'example' directory and open it in your editor as this is where we will remain for the rest of the tutorial. 

The first thing we will do in Site.java is include our dependencies.

```
package com.example;

import spark.*;
import spark.servlet.SparkApplication;
import java.util.HashMap;
import com.twocheckout.*;
import com.twocheckout.model.*;
```

Next we create the class and define a new 'get' route for the index and a new 'post' route for the order processing.

```
public class Site implements SparkApplication {

    @Override
    public void init() {

        Spark.get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {

            }
        });

        Spark.post(new Route("/order") {
            @Override
            public Object handle(Request request, Response response) {

            }
        });

    }
}
```

----

# Create a Token

For the sake of simplicity, we will have spark output our HTML and JavaScript for the credit card form directly when the index route is hit. Go ahead and add the following with-in the index route and replace 'sandbox-seller-id' and 'sandbox-publishable-key' with your credentials.

```
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
```

Let's take a second to look at what we did here. 

We created a basic credit card form that allows our buyer to enter in their card number, expiration month and year and CVC. Notice that we have a no 'name' attributes on the input elements that collect the credit card information. This will insure that no sensitive card data touches your server when the form is submitted. Also, we include a hidden input element for the token which we will submit to our server to make the authorization request.

Then we pulled in a jQuery library to help us with manipulating the document.
(The 2co.js library does NOT require jQuery.)

We also pulled in the 2co.js library so that we can make our token request with the card details.

```
<script src="https://www.2checkout.com/checkout/api/2co.min.js"></script>
```

This library provides us with 2 functions, one to load the public encryption key, and one to make the token request.

The `TCO.loadPubKey(String environment, Function callback)` function must be used to asynchronously load the public encryption key for the 'production' or 'sandbox' environment. In this example, we are going to call this as soon as the document is ready so it is not necessary to provide a callback.

```
TCO.loadPubKey('sandbox');
```

The the 'TCO.requestToken(Function callback, Function callback, Object arguments)' function is used to make the token request. This function takes 3 arguments:

* Your success callback function which accepts one argument and will be called when the request is successful.
* Your error callback function which accepts one argument and will be called when the request results in an error.
* An object containing the credit card details and your credentials.
    * **sellerId** : 2Checkout account number
    * **publishableKey** : Payment API publishable key
    * **ccNo** : Credit Card Number
    * **expMonth** : Card Expiration Month
    * **expYear** : Card Expiration Year
    * **cvv** : Card Verification Code

```
TCO.requestToken(successCallback, errorCallback, args);
```




In our example we created 'tokenRequest' function to setup our arguments by pulling the values entered on the credit card form and we make the token request.

```
var tokenRequest = function() {
    // Setup token request arguments
    var args = {
        sellerId: "sandbox-seller-id",
        publishableKey: "sandbox-publishable-key",
        ccNo: $("#ccNo").val(),
        cvv: $("#cvv").val(),
        expMonth: $("#expMonth").val(),
        expYear: $("#expYear").val()
    };

    // Make the token request
    TCO.requestToken(successCallback, errorCallback, args);
};
```

We then call this function from a submit handler function that we setup on the form.

```
$("#myCCForm").submit(function(e) {
    // Call our token request function
    tokenRequest();

    // Prevent form from submitting
    return false;
});
```

The 'successCallback' function is called if the token request is successful. In this function we set the token as the value for our 'token' hidden input element and we submit the form to our server.

```
var successCallback = function(data) {
    var myForm = document.getElementById('myCCForm');

    // Set the token as the value for the token input
    myForm.token.value = data.response.token.token;

    // IMPORTANT: Here we call `submit()` on the form element directly instead of using jQuery to prevent and infinite token request loop.
    myForm.submit();
};
```

The 'errorCallback' function is called if the token request fails. In our example function, we check for error code 200, which indicates that the ajax call has failed. If the error code was 200, we automatically re-attempt the tokenization, otherwise, we alert with the error message.

```
var errorCallback = function(data) {
    if (data.errorCode === 200) {
        tokenRequest();
    } else {
        alert(data.errorMsg);
    }
};
```

----

# Create the Sale

In the order route, we will use the token passed from our credit card form to submit the authorization request and display the response.
(Be sure to replace 'sandbox-private-key' and 'sandbox-seller-id' with your sandbox credentials.)

```
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
    billing.put("email", "tester@2co.com");
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
```

Lets break down this function a bit and explain what were doing here.

First we setup our credentials and the environment by setting the following:

* Twocheckout.privatekey: Your Payment API private key
* Twocheckout.mode: 'sandbox' or 'production' (defaults to production if not set)

Next we create a HashMap with our authorization attributes. In our example we are using hard coded strings for each required attribute except for the token which is passed in from the credit card form.

**Important Note: A token can only be used for one authorization call, and will expire after 30 minutes if not used.**

```
HashMap billing = new HashMap();
billing.put("name", "Testing Tester");
billing.put("addrLine1", "123 Test St");
billing.put("city", "Columbus");
billing.put("state", "OH");
billing.put("country", "USA");
billing.put("zipCode", "43230");
billing.put("email", "tester@2co.com");
billing.put("phone", "555-555-5555");

HashMap params = new HashMap();
params.put("sellerId", "sandbox-seller-id");
params.put("merchantOrderId", "test123");
params.put("token", request.queryParams("token"));
params.put("currency", "USD");
params.put("total", "1.00");
params.put("billingAddr", billing);
```

Finally we submit the charge using the 'TwocheckoutCharge.authorize(HashMap);' function and display the result to the buyer. It is important to wrap this in a try/catch block so that you can handle the response and catch the 'TwocheckoutException' exception that will be thrown if the card fails to authorize.

```
try {
    ...

    Authorization result = TwocheckoutCharge.authorize(params);
    message = result.getResponseMsg();
} catch (TwocheckoutException e) {
    message = e.getMessage();
}
```

----

# Run the example application

In your console, navigate to the 'payment-api' directory and startup the application with Jetty.

```
gradle jettyrun
```

In your browser, navigate to 'http://localhost:8080/payment-api/', and you should see a payment form where you can enter credit card information.

For your testing, you can use these values for a successful authorization
>Credit Card Number: 4000000000000002

>Expiration date: 10/2020

>cvv: 123

And these values for a failed authorization:

>Credit Card Number: 4333433343334333

>Expiration date: 10/2020

>cvv:123

If you have any questions, feel free to send them to techsupport@2co.com
