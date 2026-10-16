package com.froes.assembly.util;

import com.github.javafaker.Faker;
import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.co.jemos.podam.typeManufacturers.LongTypeManufacturerImpl;
import uk.co.jemos.podam.typeManufacturers.StringTypeManufacturerImpl;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

public class PodamFactory {

    public static final PodamFactoryImpl podam;

    public static final Faker faker = Faker.instance(new Locale("pt-BR"));

    static {
        podam = new PodamFactoryImpl();
        podam.getStrategy().addOrReplaceTypeManufacturer(String.class, new StringTypeManufacturerImpl() {
            @Override
            public String getType(DataProviderStrategy strategy, AttributeMetadata attributeMetadata, Map<String, Type> genericTypesArgumentsMap) {
                return faker.beer().name();
            }
        }).addOrReplaceTypeManufacturer(Long.class, new LongTypeManufacturerImpl() {
            @Override
            public Long getType(DataProviderStrategy strategy, AttributeMetadata attributeMetadata, Map<String, Type> genericTypesArgumentsMap) {
                return (long) faker.number().numberBetween(0, 100);
            }
        });
    }

}
