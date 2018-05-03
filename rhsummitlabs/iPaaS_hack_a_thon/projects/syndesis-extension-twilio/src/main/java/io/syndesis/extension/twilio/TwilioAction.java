package io.syndesis.extension.twilio;

import java.util.Map;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.component.twilio.TwilioComponent;
import org.apache.camel.component.twilio.TwilioConfiguration;
import org.apache.camel.component.twilio.internal.TwilioApiName;
import org.apache.camel.model.ProcessorDefinition;

import io.syndesis.extension.api.Step;
import io.syndesis.extension.api.annotations.Action;
import io.syndesis.extension.api.annotations.ConfigurationProperty;
import io.syndesis.extension.api.annotations.DataShape;

@Action(	id = "twilio", 
			name = "twilio", 
			description = "Send Message through Twilio", 
			tags = { "twilio", "extension"}, 
			inputDataShape = @DataShape(name = "twilioMsg",
										description = "Text Message content to send via Twilio",
										kind = "java",
										type="io.syndesis.extension.twilio.TwilioMsg") 
)
public class TwilioAction implements Step {
	TwilioConfiguration twilioConfiguration = new TwilioConfiguration();
	
	@ConfigurationProperty(
        name = "message",
        description = "The logging message",
        displayName = "message",
        type = "string")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    @ConfigurationProperty(
            name = "phoneto",
            description = "Phone number message is sending to",
            displayName = "To",
            type = "string")
    private String phoneto;

    public String getPhoneto() {
    	return phoneto;
    }

    public void setPhoneto(String phoneto) {
    	this.phoneto = phoneto;
    }

    
    
    private TwilioMsg twilioMsg;
    
    public void setTwilioMsg(TwilioMsg twilioMsg) {
    	this.twilioMsg = twilioMsg;
    }
    
    public TwilioMsg getTwilioMsg() {
    	return twilioMsg;
    }
    
    @Override
    public Optional<ProcessorDefinition> configure(CamelContext context, ProcessorDefinition route, Map<String, Object> parameters) {
        
        //Preset Account so user doesn't have to do it 
        twilioConfiguration.setAccountSid("YOUR TWILIO AccountSid ");
        twilioConfiguration.setPassword("YOUR TWILIO PWD");
        twilioConfiguration.setUsername("USER NAME");
        twilioConfiguration.setApiName(TwilioApiName.MESSAGE);
        twilioConfiguration.setMethodName("CREATE");
        
        TwilioComponent twilioComponent = new TwilioComponent();
        twilioComponent.setConfiguration(twilioConfiguration);
        
        context.addComponent("twilio", twilioComponent);
        
        if((message != null && !"".equals(message.trim())) && (phoneto != null && !"".equals(phoneto.trim()))) {
        	return Optional.of(route.to("twilio://message/create?from=+16177122515&to="+phoneto+"&body='"+message+"'"));
        }
        else {
        	return Optional.of(
        			route.toD("twilio://message/create?from=+16177122515&to=${body.phoneNotoSend}&body=${body.textmsg}"));
        	
        }
   }
    
    
}

