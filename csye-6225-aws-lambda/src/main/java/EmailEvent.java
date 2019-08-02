import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

public class EmailEvent implements RequestHandler<SNSEvent, Object> {
    public Object handleRequest(SNSEvent request, Context context) {
        //domain and fromemail has to taken from environment variable

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation started: " + timeStamp);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("csye6225");

        UUID token = UUID.randomUUID();
        String toEmail= request.getRecords().get(0).getSNS().getMessage();
        long currentEpochTime= System.currentTimeMillis() / 1000L;
        long expirationTime = currentEpochTime + 60;
        String fromEmail="yogita@csye6225-su19-patilyo.me";
        context.getLogger().log(toEmail);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("Email = :email")
                .withFilterExpression("ttl_timestamp > :ttl")
                .withValueMap(new ValueMap()
                        .withString(":email", toEmail)
                        .withNumber(":ttl", currentEpochTime))
                .withConsistentRead(true);
        ItemCollection<QueryOutcome> items = table.query(spec);
        if(items.getTotalCount() == 0){
            sendEmail(fromEmail, toEmail, String.valueOf(token), context);
            Item item = new Item()

                    .withPrimaryKey("Email", toEmail)

                    .withString("Token", String.valueOf(token))

                    .withNumber("ttl_timestamp", expirationTime);

            PutItemOutcome outcome = table.putItem(item);
            //context.getLogger().log(outcome.getItem().toJSONPretty());
        }

        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
        context.getLogger().log("Invocation completed: " + timeStamp);
        return null;
    }

    private void sendEmail(String fromEmail, String toEmail, String token, Context context){
        try {
            String domain= "example.com";
            String TEXTBODY="http://"+ domain +"/reset?email="+ toEmail + "&token=" + token;
            String HTMLBODY="<p>"+TEXTBODY+"<p>";

            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();

            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(toEmail))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(HTMLBODY))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(TEXTBODY)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData("Reset Password")))
                    .withSource(fromEmail);
            client.sendEmail(request);
            context.getLogger().log("Email sent!");
        } catch (Exception ex) {
            context.getLogger().log("The email was not sent. Error message: "
                    + ex.getMessage());
        }
    }
}