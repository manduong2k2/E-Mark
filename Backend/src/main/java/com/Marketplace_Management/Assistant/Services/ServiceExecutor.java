
package com.Marketplace_Management.Assistant.Services;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;

import com.Marketplace_Management.Assistant.Services.ChatService.ResolvedIntent;
import com.Marketplace_Management.Auth.Contracts.IUserService;
import com.Marketplace_Management.Catalog.Contracts.IBrandService;
import com.Marketplace_Management.Catalog.Contracts.ICategoryService;
import com.Marketplace_Management.Catalog.Contracts.IProductService;
import com.Marketplace_Management.Order.Contracts.IOrderService;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;

import tools.jackson.databind.ObjectMapper;

@Service
public class ServiceExecutor {

    private final Map<String, Object> serviceRegistry;
    private final ObjectMapper objectMapper;

    private static final Map<String, Set<String>> ALLOWED_METHODS = Map.of(
            "products", Set.of("getAll", "getById"),
            "orders", Set.of("getAll", "getById", "myOrders"),
            "users", Set.of("getAll", "getById"),
            "brands", Set.of("getAll", "getById"),
            "categories", Set.of("getAll", "getById"),
            "vendors", Set.of("getAll", "getById")
    );

    public ServiceExecutor(
            IProductService productService,
            IOrderService orderService,
            IUserService userService,
            IBrandService brandService,
            ICategoryService categoryService,
            IVendorService vendorService,
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        this.serviceRegistry = Map.of(
                "products", productService,
                "orders", orderService,
                "users", userService,
                "brands", brandService,
                "categories", categoryService,
                "vendors", vendorService
        );
    }

    public Object execute(ResolvedIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException(
                    "ResolvedIntent must not be null");
        }

        String domain = intent.domain();
        String methodName = intent.method();

        Map<String, Object> params = intent.params() == null
                ? Map.of()
                : intent.params();

        // 1. Resolve service.
        Object service = serviceRegistry.get(domain);

        if (service == null) {
            throw new IllegalArgumentException(
                    "Unsupported domain: " + domain);
        }

        // 2. Validate allowlist.
        Set<String> allowedMethods = ALLOWED_METHODS.get(domain);

        if (methodName == null
                || allowedMethods == null
                || !allowedMethods.contains(methodName)) {
            throw new IllegalArgumentException(
                    "Unsupported method: " + domain + "." + methodName);
        }

        // 3. Find the method exposed by the service interface.
        Method targetMethod = findMethod(service, methodName);

        // 4. Convert params to the method arguments.
        Object[] args = buildArguments(targetMethod, params);

        // 5. Execute.
        try {
            return targetMethod.invoke(service, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to execute " + domain + "." + methodName, e);
        }
    }

    private Method findMethod(Object service, String methodName) {
        Class<?> targetClass = AopUtils.getTargetClass(service);

        // Prefer the service interface to avoid CGLIB proxy methods.
        return Arrays.stream(targetClass.getInterfaces())
                .flatMap(iface -> Arrays.stream(iface.getMethods()))
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == 1)
                .findFirst()
                .orElseGet(() ->
                        Arrays.stream(service.getClass().getInterfaces())
                                .flatMap(iface -> Arrays.stream(iface.getMethods()))
                                .filter(method -> method.getName().equals(methodName))
                                .filter(method -> method.getParameterCount() == 1)
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "No matching method found: "
                                                + service.getClass().getSimpleName()
                                                + "." + methodName
                                                + " with one parameter")));
    }

    private Object[] buildArguments(
            Method method,
            Map<String, Object> params) {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        if (parameters.length == 0) {
            if (!params.isEmpty()) {
                throw new IllegalArgumentException(
                        "Method " + method.getName()
                                + " does not accept parameters");
            }
            return args;
        }

        if (parameters.length == 1) {
            Class<?> parameterType = parameters[0].getType();

            // DTO/Command: convert the entire params map.
            if (!isSimpleType(parameterType)) {
                args[0] = objectMapper.convertValue(params, parameterType);
                return args;
            }
        }

        // Methods with individual parameters.
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (!parameter.isNamePresent()) {
                throw new IllegalArgumentException(
                        "Parameter names unavailable for "
                                + method.getName()
                                + ". Compile with -parameters or use a DTO.");
            }

            String name = parameter.getName();

            if (!params.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Missing required parameter: " + name);
            }

            args[i] = convertValue(
                    params.get(name),
                    parameter.getType());
        }

        return args;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || type == UUID.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException(
                        "Cannot pass null to primitive type: "
                                + targetType.getName());
            }
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == UUID.class) {
            return UUID.fromString(value.toString());
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value.toString());
        }

        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value.toString());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value.toString());
        }

        if (targetType.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object enumValue = Enum.valueOf(
                    (Class<? extends Enum>) targetType,
                    value.toString());
            return enumValue;
        }

        // Fallback for other supported Jackson-convertible types.
        return objectMapper.convertValue(value, targetType);
    }
}
