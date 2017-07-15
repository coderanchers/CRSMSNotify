package com.coderanchers.sms.notify;

import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import com.coderanchers.sms.notify.dao.CRSMSNotifyDAO;

@Path("sms")
public class CRSMSNotify {

	private static final String FROM = "From=";
	private static final String ACCOUNT_SID = "AccountSid=";
	private static final String BODY = "Body=";
	private static final String TO = "To=";
	private CRSMSNotifyDAO dao = new CRSMSNotifyDAO();
	
	public static final String TWILIO_ACCOUNT_SID_KEY = "sid";
	public static final String TWILIO_NUMBER_KEY = "twilioNumber";
	
	@POST
	public Response receiveMessage(String sms) {
		
		
		Map<String, String> twilioCredentials = dao.getCredentials();
		if(twilioCredentials.isEmpty()) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		final String myTwilioAccountSID = twilioCredentials.get(TWILIO_ACCOUNT_SID_KEY);
		final String myTwilioNumber = twilioCredentials.get(TWILIO_NUMBER_KEY);
		
		String accountString = sms.substring(sms.indexOf(ACCOUNT_SID));
		String accountSid = accountString.substring(0, accountString.indexOf("&"));
		String smsAccountSid = accountSid.substring(ACCOUNT_SID.length());
		
		if(!myTwilioAccountSID.equalsIgnoreCase(smsAccountSid)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String toString = sms.substring(sms.indexOf(TO));
		String to = toString.substring(0, toString.indexOf("&"));
		String smsNumber = to.substring(TO.length());
		
		if(!myTwilioNumber.equalsIgnoreCase(smsNumber.replaceAll("%2B", "+"))) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String fromNumberString = sms.substring(sms.indexOf(FROM));
		String fromNumber = fromNumberString.substring(0, fromNumberString.indexOf("&"));
		String from = fromNumber.substring(FROM.length()).replaceAll("%2B", "+");

		String commentString = sms.substring(sms.indexOf(BODY));
		String commentText = commentString.substring(0, commentString.indexOf("&"));
		String comment = commentText.substring(BODY.length()).replaceAll("%3A", ":");

		boolean success = postComment(from, comment);
		if(success) {
			return Response.ok().build();
		}
		else return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	private boolean postComment(String from, String comment) {

		String postId = "";
		String trimmedComment = "";
		String userName = dao.getUserFromMobile(from);
		
		try {
			
		    postId = comment.substring(0, comment.indexOf("::"));
		    trimmedComment = comment.substring(comment.indexOf("::")+2);
		    trimmedComment += "-Posted By " + userName;
		    
		}catch (Exception ex) {
			System.out.println("Couldn't get post from comment - aborting");
			return false;
		}
		
		try {
			
			String wordpressURL = dao.getWordPressURL();
			String queryParams = "post="+postId+"&content="+cleanUp(trimmedComment);
			wordpressURL += "?"+queryParams;

			ClientRequest request = new ClientRequest(wordpressURL);
			ClientResponse<String> response = request.post(String.class);
			System.out.println(response.getStatus());
			
		  } catch (Exception e) {
			System.out.println("Exception posting comment to WordPress " + e.getMessage());
			return false;
		  }
		return true;
	}

	private static String cleanUp(String trimmedComment) {
		String cleanedComment = trimmedComment.replaceAll("\\+", "%20");
		return cleanedComment;
	}

}
