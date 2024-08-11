package com.gitlab.progxaker.otelextension;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Locale;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SparkProcAppInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.apache.livy.utils.SparkProcApp");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        /*
            TODO: Use the following objects to set a Span attribute
                (!) .or(named("changeState"))
                (?) .or(named("state"))
                (?) .or(named("kill"))
        */
        transformer.applyAdviceToMethod(
            named("$anonfun$waitThread$1"),
            this.getClass().getName() + "$AnonFunctionAdvice");

        // Scala "org.apache.livy.utils.SparkApp$State$Value" translates to Java "scala.Enumeration$Value"
        transformer.applyAdviceToMethod(
            isMethod()
              .and(takesArguments(1))
              .and(takesArgument(0, named("scala.Enumeration$Value")))
              .and(named("changeState")),
            this.getClass().getName() + "$ChangeStateAdvice");
    }

    @SuppressWarnings("unused")
    public static class AnonFunctionAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(@Advice.Local("otelSpan") Span span,
                                   @Advice.Local("otelScope") Scope scope) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("apache-livy", "0.8");
            SpanBuilder spanBuilder = tracer.spanBuilder("Run a Spark application");

            String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("SparProcApp_")) {
                SpanContext livyAppContext = SpanContextStorage.get(threadName);
                if (livyAppContext != null) {
                    spanBuilder.addLink(livyAppContext);
                    SpanContextStorage.remove(threadName);
                }
            }

            span = spanBuilder.startSpan();
            scope = span.makeCurrent();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(@Advice.Local("otelSpan") Span span,
                                  @Advice.Local("otelScope") Scope scope,
                                  @Advice.Thrown Throwable throwable) {
            if (scope == null) {
                return;
            }
            scope.close();
            span.end();
        }
    }

    @SuppressWarnings("unused")
    public static class ChangeStateAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void onExit(@Advice.FieldValue("state") Object finalState,
                                  @Advice.Argument(0) Object specifiedState) {
            /*
                TODO: Use ConcurrentHashMap to set the state attribute only for the root span
                ---
                The function compares the final value (i.e. the state that was set
                by the changeState function) with the input argument. If they match,
                it means that the function has set a new value, so it's necessary
                to set it in the state attribute.
            */
            if (finalState.equals(specifiedState)) {
                Span span = Span.current();
                if (span != null) {
                    span.setAttribute("state", specifiedState.toString().toLowerCase(Locale.ROOT));
                }
            }
        }
    }
}
