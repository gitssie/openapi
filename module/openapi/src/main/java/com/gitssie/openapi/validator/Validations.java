package com.gitssie.openapi.validator;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Validations {
    public static final Validations INSTANCE = new Validations();

    private Map<String,ValidatorImpl> validateRules = Maps.newHashMap();

    public Validations(){
        validateRules.put("validate.BytesRules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        /**
        validateRules.put("validate.BytesRules.len",(f,v,e) -> BytesValidation.length(f,(ByteString)v,((Long)e).intValue()));
        validateRules.put("validate.BytesRules.min_len",(f,v,e) -> BytesValidation.minLength(f,(ByteString)v,((Long)e).intValue()));
        validateRules.put("validate.BytesRules.max_len",(f,v,e) -> BytesValidation.maxLength(f,(ByteString)v,((Long)e).intValue()));
        validateRules.put("validate.BytesRules.pattern",(f,v,e) -> BytesValidation.pattern(f,(ByteString)v,Pattern.compile((String)e)));
        validateRules.put("validate.BytesRules.prefix",(f,v,e) -> BytesValidation.prefix(f,(ByteString)v,((ByteString)e).toByteArray()));
        validateRules.put("validate.BytesRules.suffix",(f,v,e) -> BytesValidation.suffix(f,(ByteString)v,((ByteString)e).toByteArray()));
        validateRules.put("validate.BytesRules.contains",(f,v,e) -> BytesValidation.contains(f,(ByteString)v,((ByteString)e).toByteArray()));
         **/
        validateRules.put("validate.BytesRules.in",(f,v,e) -> CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.BytesRules.not_in",(f,v,e) ->  CollectiveValidation.notIn(f,v,((List) e).toArray()));
        /**
        validateRules.put("validate.BytesRules.ip",(f,v,e) -> BytesValidation.ip(f,(ByteString)v));
        validateRules.put("validate.BytesRules.ipv4",(f,v,e) -> BytesValidation.ip(f,(ByteString)v));
        validateRules.put("validate.BytesRules.ipv6",(f,v,e) -> BytesValidation.ip(f,(ByteString)v));
        **/
        validateRules.put("validate.StringRules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        validateRules.put("validate.StringRules.len",(f,v,e)  -> StringValidation.length(f,(String)v,(Integer)e));
        validateRules.put("validate.StringRules.min_len",(f,v,e)  -> StringValidation.minLength(f,(String)v,((Long)e).intValue()));
        validateRules.put("validate.StringRules.max_len",(f,v,e)  -> StringValidation.maxLength(f,(String)v,((Long)e).intValue()));
        validateRules.put("validate.StringRules.len_bytes",(f,v,e)  -> StringValidation.lenBytes(f,(String)v,((Long)e).intValue()));
        validateRules.put("validate.StringRules.min_bytes",(f,v,e)  -> StringValidation.minBytes(f,(String)v,((Long)e).intValue()));
        validateRules.put("validate.StringRules.max_bytes",(f,v,e)  -> StringValidation.maxBytes(f,(String)v,((Long)e).intValue()));
        validateRules.put("validate.StringRules.pattern",(f,v,e)  -> StringValidation.pattern(f,(String)v, Pattern.compile((String)e)));
        validateRules.put("validate.StringRules.prefix",(f,v,e)  -> StringValidation.prefix(f,(String)v,(String)e));
        validateRules.put("validate.StringRules.suffix",(f,v,e)  -> StringValidation.suffix(f,(String)v,(String)e));
        validateRules.put("validate.StringRules.contains",(f,v,e)  -> StringValidation.contains(f,(String)v,(String)e));
        validateRules.put("validate.StringRules.not_contains",(f,v,e)  -> StringValidation.notContains(f,(String)v,(String)e));
        validateRules.put("validate.StringRules.in",(f,v,e) ->CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.StringRules.not_in",(f,v,e) -> CollectiveValidation.notIn(f,v,((List) e).toArray()));

        validateRules.put("validate.StringRules.email",(f,v,e) -> StringValidation.email(f,(String)v));
        validateRules.put("validate.StringRules.hostname",(f,v,e) -> StringValidation.hostName(f,(String)v));
        validateRules.put("validate.StringRules.ip",(f,v,e) -> StringValidation.ip(f,(String)v));
        validateRules.put("validate.StringRules.ipv4",(f,v,e) -> StringValidation.ipv4(f,(String)v));
        validateRules.put("validate.StringRules.ipv6",(f,v,e) -> StringValidation.ipv6(f,(String)v));
        validateRules.put("validate.StringRules.uri",(f,v,e) -> StringValidation.uri(f,(String)v));
        validateRules.put("validate.StringRules.uri_ref",(f,v,e) -> StringValidation.uriRef(f,(String)v));
        validateRules.put("validate.StringRules.address",(f,v,e) -> StringValidation.address(f,(String)v));
        validateRules.put("validate.StringRules.uuid",(f,v,e) -> StringValidation.uuid(f,(String)v));

        validateRules.put("validate.BoolRules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));

        validateRules.put("validate.Int64Rules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        validateRules.put("validate.Int64Rules.gt",(f,v,e) -> ComparativeValidation.greaterThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int64Rules.gte",(f,v,e) -> ComparativeValidation.greaterThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int64Rules.lt",(f,v,e) -> ComparativeValidation.lessThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int64Rules.lte",(f,v,e) -> ComparativeValidation.lessThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int64Rules.in",(f,v,e) ->CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.Int64Rules.not_in",(f,v,e) -> CollectiveValidation.notIn(f,v,((List) e).toArray()));

        validateRules.put("validate.Int32Rules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        validateRules.put("validate.Int32Rules.gt",(f,v,e) -> ComparativeValidation.greaterThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int32Rules.gte",(f,v,e) -> ComparativeValidation.greaterThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int32Rules.lt",(f,v,e) -> ComparativeValidation.lessThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int32Rules.lte",(f,v,e) -> ComparativeValidation.lessThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.Int32Rules.in",(f,v,e) -> CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.Int32Rules.not_in",(f,v,e) -> CollectiveValidation.notIn(f,v,((List) e).toArray()));

        validateRules.put("validate.FloatRules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        validateRules.put("validate.FloatRules.gt",(f,v,e) -> ComparativeValidation.greaterThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.FloatRules.gte",(f,v,e) -> ComparativeValidation.greaterThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.FloatRules.lt",(f,v,e) -> ComparativeValidation.lessThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.FloatRules.lte",(f,v,e) -> ComparativeValidation.lessThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.FloatRules.in",(f,v,e) -> CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.FloatRules.not_in",(f,v,e) -> CollectiveValidation.notIn(f,v,((List) e).toArray()));

        validateRules.put("validate.DoubleRules.const",(f,v,e) -> ConstantValidation.constant(f,v,e));
        validateRules.put("validate.DoubleRules.gt",(f,v,e) -> ComparativeValidation.greaterThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.DoubleRules.gte",(f,v,e) -> ComparativeValidation.greaterThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.DoubleRules.lt",(f,v,e) -> ComparativeValidation.lessThan(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.DoubleRules.lte",(f,v,e) -> ComparativeValidation.lessThanOrEqual(f,(Comparable)v,(Comparable)e, Comparable::compareTo));
        validateRules.put("validate.DoubleRules.in",(f,v,e) -> CollectiveValidation.in(f,v,((List) e).toArray()));
        validateRules.put("validate.DoubleRules.not_in",(f,v,e) -> CollectiveValidation.notIn(f,v,((List) e).toArray()));

    }

    public ValidatorImpl getValidator(String rule){
        return validateRules.get(rule);
    }
}
