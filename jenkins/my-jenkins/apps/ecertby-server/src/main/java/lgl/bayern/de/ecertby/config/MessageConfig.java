package lgl.bayern.de.ecertby.config;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class MessageConfig {

    private static MessageSource messageSource;

    private static final String BASENAME = "messages_DE";

    @Bean(name = "messages")
    public static PropertiesFactoryBean mapper() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("messages_DE.properties"));
        return bean;
    }

    @Bean("messageSource")
    public static MessageSource messageSource() {
        ResourceBundleMessageSource message =
                new ResourceBundleMessageSource();
        message.setBasenames(BASENAME);
        message.setDefaultEncoding("UTF-8");
        message.setDefaultLocale(AppConstants.LOCALE);
        messageSource = message;
        return message;
    }

    public static String getValue(String code, Object[] args) {
        return messageSource.getMessage(code, args, AppConstants.LOCALE);
    }
    public static String getValue(String code) {
        return messageSource.getMessage(code, null, AppConstants.LOCALE);
    }

}
