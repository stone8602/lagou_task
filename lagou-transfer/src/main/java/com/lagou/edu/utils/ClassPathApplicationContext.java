package com.lagou.edu.utils;


import com.lagou.edu.factory.ProxyFactory;
import com.lagou.edu.interfaces.*;
import com.mysql.jdbc.StringUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cain
 * @date 2020/2/26 16:18
 * @description
 */
public class ClassPathApplicationContext {
    private String packagePath;
    private static ConcurrentHashMap<String,Object> beans;
    private String factoryName="proxyFactory";

    public ClassPathApplicationContext(String packagePath) throws Exception {
        this.packagePath=packagePath;
        beans=new ConcurrentHashMap<>();
        initFactory();
        initBeans();
        initResources();
        changeToFactory();
    }

    private void initFactory() throws Exception {
        List<Class<?>> classList= ClassUtil.getClasses(packagePath);
        ConcurrentHashMap<String,Object> beans= findFactoryClass(classList);
        if(beans==null||beans.isEmpty()){
            throw new Exception("此包下没有注解的类");
        }
    }

    private void initBeans() throws Exception {
        List<Class<?>> classList= ClassUtil.getClasses(packagePath);
        ConcurrentHashMap<String,Object> beans= findClass(classList);
        if(beans==null||beans.isEmpty()){
            throw new Exception("此包下没有注解的类");
        }
    }

    private void initResources() throws Exception {
        Set<Map.Entry<String,Object>> entrySet=beans.entrySet();
        for(Map.Entry<String,Object> entry:entrySet){
            Object value=entry.getValue();
            autowried(value);
        }
    }

    private ConcurrentHashMap findFactoryClass(List<Class<?>> classList) throws IllegalAccessException, InstantiationException {
        for(Class<?> clazz:classList){
            Component annotation=clazz.getDeclaredAnnotation(Component.class);
            if(annotation!=null){
                String className=clazz.getSimpleName();
                String beanKey=annotation.value();
                if(StringUtils.isNullOrEmpty(beanKey)){
                    //首字母小写
                    beanKey=firstToLowerCase(className);
                }
                Object object=clazz.newInstance();
                beans.put(beanKey,object);
            }
        }
        return beans;
    }

    private ConcurrentHashMap findClass(List<Class<?>> classList) throws IllegalAccessException, InstantiationException {
        for(Class<?> clazz:classList){
            Service annotation=clazz.getDeclaredAnnotation(Service.class);
            if(annotation!=null){
                instanceBean(clazz,annotation.value());
            }else {
                Repository repository=clazz.getDeclaredAnnotation(Repository.class);
                if(repository!=null){
                    instanceBean(clazz,repository.value());
                }
            }
        }
        return beans;
    }

    private void autowried(Object object) throws Exception {
        Class<? extends  Object> clazz=object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        if(fields!=null&&fields.length>0){
            for(Field field:fields){
                Autowired autowired=field.getDeclaredAnnotation(Autowired.class);
                if(autowired!=null){
                    String name=field.getName();
                    Object bean=getBean(name);
                    field.setAccessible(true);
                    field.set(object,bean);
                }
            }
        }
    }

    public Object getBean(String beanId) throws Exception {
        if(StringUtils.isNullOrEmpty(beanId)){
            throw new Exception("beanId不可为空");
        }
        return beans.get(beanId);
    }

    public String firstToLowerCase(String className){
        if(Character.isLowerCase(className.charAt(0))){
            return className;
        }
        return new StringBuilder().append(Character.toLowerCase(className.charAt(0))).append(className.substring(1)).toString();
    }

    private void instanceBean(Class clazz,String beanKey) throws IllegalAccessException, InstantiationException {
        String className=clazz.getSimpleName();
        if(StringUtils.isNullOrEmpty(beanKey)){
            //首字母小写
            beanKey=firstToLowerCase(className);
        }
        Object object=clazz.newInstance();
        beans.put(beanKey,object);
    }

    /**
     * 检查类中的方法是否使用了@Transactional注解，如果是，替换为代理工厂生成的bean
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void changeToFactory() throws IllegalAccessException, InstantiationException {
        ProxyFactory proxyFactory= (ProxyFactory) beans.get(factoryName);
        for(String beanKey:beans.keySet()){
            Object object=beans.get(beanKey);
            Class<?>[] interfaces = object.getClass().getInterfaces();
            Method[] methods = object.getClass().getMethods();
            boolean flag=false;
            for(Method method:methods){
                Transactional annotation = method.getAnnotation(Transactional.class);
                if(annotation!=null){
                    flag=true;
                }
            }
            if(interfaces!=null&&interfaces.length>0){
                if(flag){
                    object=proxyFactory.getJdkProxy(beans.get(beanKey));
                }else {
                    object=proxyFactory.getCglibProxy(beans.get(beanKey));
                }
                beans.put(beanKey,object);
            }
        }
    }

}
