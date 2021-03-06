package com.redhat.lightblue.util.stopwatch;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect with pointcut for all methods annotated with @StopWatch. Measures execution time and logs a warning if it's higher than threshold.
 *
 * @author mpatercz
 *
 */
@Aspect
public class StopWatchAspect {

    private static final Logger logger = LoggerFactory.getLogger(StopWatchAspect.class);

    static StopWatchLogger stopWatchLogger = new StopWatchLogger();

    @Around("@annotation(StopWatch) && execution(* *(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        if (!enabled()) {
            return joinPoint.proceed();
        }

        StopWatch stopWatch = getAnnotation(joinPoint);
        String loggerName = stopWatch.loggerName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        @SuppressWarnings("rawtypes")
        SizeCalculator calc = getSizeCalculator(stopWatch);

        final long start = System.nanoTime();

        try {
            Object returned = joinPoint.proceed();

            // calculate result size and log a warning if exceeds threshold
            if (returned != null && calc != null) {
                try {
                    @SuppressWarnings("unchecked")
                    int size = calc.size(returned);
                    if (warnSizeThresholdB(joinPoint) >= 0 && size >= warnSizeThresholdB(joinPoint)) {
                        stopWatchLogger.warn(loggerName, "call="+className+Mnemos.toText(joinPoint, false, false)+" resultSize="+size);
                    }
                } catch (Exception e) {
                    // swallow this exception to avoid potential SizeCalculator bugs to impact request processing
                    logger.error("Error when calculating result size!", e);
                }
            }

            return returned;
        } catch (Throwable t) {
            throw t;
        } finally {
            final long tookNano = System.nanoTime() - start;
            final long tookMS = TimeUnit.MILLISECONDS.convert(tookNano, TimeUnit.NANOSECONDS);

            if (tookMS >= warnThresholdMS(joinPoint)) {
                stopWatchLogger.warn(loggerName, "call="+className+Mnemos.toText(joinPoint, false, false)+" executionTimeMS="+tookMS);
            }

            if (stopWatchLogger.isDebugEnabled(loggerName)) {
                stopWatchLogger.debug(loggerName, "call="+className+Mnemos.toText(joinPoint, false, false)+" executionTimeMS="+tookMS);
            }
        }
    }

    public static final String ENABLED_PROP = "stopwatch.enabled", GLOBAL_WARN_THRESHOLD_MS_PROP = "stopwatch.globalWarnThresholdMS",
            GLOBAL_WARN_SIZE_THRESHOLD_B_PROP = "stopwatch.globalWarnSizeThresholdB";

    private StopWatch getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return method.getAnnotation(StopWatch.class);
    }

    @SuppressWarnings("rawtypes")
    private SizeCalculator getSizeCalculator(StopWatch stopWatch) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String sizeCalculatorClass = stopWatch.sizeCalculatorClass();

        return "null".equals(sizeCalculatorClass) ? null : (SizeCalculator)Class.forName(sizeCalculatorClass).asSubclass(SizeCalculator.class).newInstance();
    }

    private static Boolean enabled = null;
    private static Integer globalWarnThresholdMS = null;
    private static Integer globalWarnSizeThresholdB = null;

    private boolean enabled() {
        if (enabled == null) {
            enabled = Boolean.parseBoolean(System.getProperty(ENABLED_PROP, "false"));
            globalWarnThresholdMS = Integer.parseInt(System.getProperty(GLOBAL_WARN_THRESHOLD_MS_PROP, "5000"));
            globalWarnSizeThresholdB = Integer.parseInt(System.getProperty(GLOBAL_WARN_SIZE_THRESHOLD_B_PROP, "-1"));

            logger.info("StopWatch initialized: enabled={}, globalWarnThresholdMS={}, globalWarnSizeThresholdB={}", enabled, globalWarnThresholdMS, globalWarnSizeThresholdB);
        }
        return enabled;
    }

    private int warnThresholdMS(ProceedingJoinPoint joinPoint) {
        int annotationWarnThresholdMS = getAnnotation(joinPoint).warnThresholdMS();

        if (annotationWarnThresholdMS < 0) {
            return globalWarnThresholdMS;
        } else {
            return annotationWarnThresholdMS;
        }
    }

    private int warnSizeThresholdB(ProceedingJoinPoint joinPoint) {
        int annotationWarnSizeThresholdB = getAnnotation(joinPoint).warnThresholdSizeB();

        if (annotationWarnSizeThresholdB < 0) {
            return globalWarnSizeThresholdB;
        } else {
            return annotationWarnSizeThresholdB;
        }
    }

    // for testing only
    static void clear() {
        System.clearProperty(ENABLED_PROP);
        System.clearProperty(GLOBAL_WARN_THRESHOLD_MS_PROP);
        System.clearProperty(GLOBAL_WARN_SIZE_THRESHOLD_B_PROP);

        enabled = null;
        globalWarnThresholdMS = null;
        globalWarnSizeThresholdB = null;
    }



}
