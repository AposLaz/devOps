package lgl.bayern.de.ecertby.model.util;

import org.apache.commons.lang.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LoggingAspect {

    @Value("${ecert.debug-methods}")
    private boolean debugMethods;

    @Around("execution(* lgl.bayern.de.ecertby.resource..*.*(..)) || execution(* lgl.bayern.de.ecertby.service..*.*(..))")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable{
        final Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass().getName());
        Object retVal = null;

        try {
            StringBuffer startMessageStringBuffer = new StringBuffer();
            StringBuffer arguments = new StringBuffer();

            startMessageStringBuffer.append("Start method ");
            startMessageStringBuffer.append(joinPoint.getSignature().getName());
            startMessageStringBuffer.append("(");

            if(debugMethods) {
                Object[] args = joinPoint.getArgs();
                for (int i = 0; i < args.length; i++) {
                    arguments.append(args[i]).append(",");
                }
                startMessageStringBuffer.append(arguments);

                if (args.length > 0) {
                    startMessageStringBuffer.deleteCharAt(startMessageStringBuffer.length() - 1);
                }
            }

            startMessageStringBuffer.append(")");

            logger.info(startMessageStringBuffer.toString());

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            retVal = joinPoint.proceed();

            stopWatch.stop();

            StringBuffer endMessageStringBuffer = new StringBuffer();
            endMessageStringBuffer.append("Finish method ");
            endMessageStringBuffer.append(joinPoint.getSignature().getName());
            endMessageStringBuffer.append("(");
            endMessageStringBuffer.append(arguments);
            endMessageStringBuffer.append(") execution time: ");
            endMessageStringBuffer.append(stopWatch.getTime());
            endMessageStringBuffer.append(" ms;");

            logger.debug(endMessageStringBuffer.toString());
        } catch (Throwable ex) {
            throw ex;
        }

        return retVal;
    }
}
