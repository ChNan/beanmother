package io.beanmother.core.mapper.setter;

import com.google.common.reflect.TypeToken;
import io.beanmother.core.converter.Converter;
import io.beanmother.core.fixture.FixtureList;
import io.beanmother.core.fixture.FixtureMap;
import io.beanmother.core.fixture.FixtureTemplate;
import io.beanmother.core.fixture.FixtureValue;
import io.beanmother.core.mapper.AbstractPropertyMapper;
import io.beanmother.core.mapper.FixtureMappingException;
import io.beanmother.core.util.PrimitiveTypeUtils;
import io.beanmother.core.util.TypeTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractFixtureSetterMapper<T extends FixtureTemplate> extends AbstractPropertyMapper<T> {
    private static Logger logger = LoggerFactory.getLogger(AbstractFixtureSetterMapper.class);

    private static String SETTER_PREFIX = "set";

    private SetterMapperMediator setterMapperMediator;

    public AbstractFixtureSetterMapper(SetterMapperMediator setterMapperMediator) {
        super(setterMapperMediator.getConverterFactory());
        this.setterMapperMediator = setterMapperMediator;
    }

    public SetterMapperMediator getSetterMapperMediator() {
        return setterMapperMediator;
    }

    protected List<Method> findSetterCandidates(Object target, String key) {
        Method[] methods = target.getClass().getMethods();
        List<Method> result = new ArrayList<>();
        for (Method method : methods) {
            String name = method.getName();
            if(name.indexOf(SETTER_PREFIX) == 0) {
                if (name.substring(SETTER_PREFIX.length(), name.length()).equalsIgnoreCase(key)) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    /**
     * Convert the fixtureValue to the given TypeToken
     * @param fixtureValue
     * @param typeToken
     * @return converted object from fixtureValue.
     */
    protected Object convert(final FixtureValue fixtureValue, TypeToken<?> typeToken) {
        if (typeToken.isPrimitive()) {
            Class<?> wrapperClass = PrimitiveTypeUtils.toWrapper((Class<?>) typeToken.getType());
            typeToken = TypeToken.of(wrapperClass);
        }

        Object source = fixtureValue.getValue();
        Converter convert = getConverterFactory().get(source, typeToken);

        if (convert != null) {
            return convert.convert(source, typeToken);
        } else {
            return null;
        }
    }

    /**
     * Convert the fixtureList to the given TypeToken
     * @param fixtureList
     * @param typeToken
     * @return converted object from fixtureList.
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected Object convert(FixtureList fixtureList, TypeToken<?> typeToken) throws IllegalAccessException, InstantiationException {
        boolean isArray = typeToken.isArray();
        boolean isList = typeToken.isSubtypeOf(TypeToken.of(List.class));

        if (!isList && !isArray) {
            throw new FixtureMappingException("Target setter of '" + fixtureList.getFixtureName() + "' must be List or array.");
        }

        final List convertedList;
        if (isArray || typeToken.getRawType().isInterface()) {
            convertedList = new ArrayList();
        } else {
            convertedList = (List) typeToken.getRawType().newInstance();
        }
        TypeToken<?> elementTypeToken = TypeTokenUtils.extractElementTypeToken(typeToken);


        for (FixtureTemplate template : fixtureList) {
            Object converted;
            if (template instanceof FixtureMap) {
                converted = convert((FixtureMap) template, elementTypeToken);
            } else if (template instanceof FixtureList) {
                converted = convert((FixtureList) template, elementTypeToken);
            } else if (template instanceof FixtureValue) {
                converted = convert((FixtureValue) template, elementTypeToken);
            } else {
                converted = null;
            }

            if (converted != null) {
                convertedList.add(converted);
            } else {
                logger.warn("Can not find converter for " + fixtureList.getFixtureName());
            }
        }

        // not found converter
        if (convertedList.size() == 0) return null;

        if(isArray) {
            if (elementTypeToken.isPrimitive()) {
                return PrimitiveTypeUtils.toWrapperListToPrimitiveArray(convertedList, (Class<?>) elementTypeToken.getType());
            } else {
                return Arrays.copyOf(convertedList.toArray(), convertedList.size(), (Class) typeToken.getRawType());
            }
        } else {
            return convertedList;
        }
    }

    /**
     * Convert FixtureMap to given type
     * @param fixtureMap
     * @param typeToken
     * @return converted Object from fixtureMap
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Object convert(FixtureMap fixtureMap, TypeToken<?> typeToken) throws IllegalAccessException, InstantiationException {
        if (typeToken.isSubtypeOf(Map.class)) {
            throw new NotImplementedException();
        } else {
            Object obj = typeToken.getRawType().newInstance();

            for (Map.Entry<String, FixtureTemplate> entry : fixtureMap.entrySet()) {
                if (entry.getValue() instanceof FixtureMap) {
                    setterMapperMediator.getFixtureMapPropertyMapper().map(obj, entry.getKey(), (FixtureMap) entry.getValue());
                } else if (entry.getValue() instanceof FixtureList) {
                    setterMapperMediator.getFixtureListPropertyMapper().map(obj, entry.getKey(), (FixtureList) entry.getValue());
                } else if (entry.getValue() instanceof FixtureValue) {
                    setterMapperMediator.getFixtureValuePropertyMapper().map(obj, entry.getKey(), (FixtureValue) entry.getValue());
                } else {
                    logger.error("can not find fixture template type");
                }
            }

            return obj;
        }
    }
}