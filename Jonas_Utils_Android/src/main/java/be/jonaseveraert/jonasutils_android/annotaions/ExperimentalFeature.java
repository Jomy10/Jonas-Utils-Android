package be.jonaseveraert.jonasutils_android.annotaions;

import androidx.annotation.experimental.Experimental;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD, CONSTRUCTOR, FIELD, PACKAGE})
@Experimental (level = Experimental.Level.WARNING)
public @interface ExperimentalFeature { }
