项目名:自动埋点

项目介绍:
    通过插装方法，实现自动埋点，在需要获取方法信息的时候，可通过服务端下发指令，
    来根据方法的参数和返回值来判断方法中可能存在的问题。

使用方式:

一.添加依赖
annotationProcessor  project(':TrackerAnnotation')
implementation project(':Tracker-Android')  //android

二.初始化，在application中添加如下初始化，addTracker用来表示需要添加的埋点，uploadLogInfo用来自定义上传接口。
如果addTracker返回true则不会执行uploadLogInfo方法,当返回false的时候，下次进入app会统一请求uploadLogInfo，并根据listener判断上传失败或成功

Trackers.instance().setTrackerAddListener(new TrackerAddListener() {
    @Override
    public boolean addTracker(String className, String methodName,String tag，Object... args) {
        return false;
    }
}).setTrackerUploadListener(new DefaultTrackerUploadListener() {
    @Override
    public void uploadLogInfo(List<TraceLog> list, TrackerResultListener listener) {
        super.uploadLogInfo(list, listener);
        String json = TrackerUtils.getTraceLogJson(list);
        if(listener!=null){
            listener.onSuccess();
        }
    }
}).setDebug(BuildConfig.DEBUG).init();


三.在需要的类或方法上用Tracker去注解，同时Tracker有几个参数可选
1.enable  是否开启
2.group  当前所属分组
3.tag  标签信息
4.injectorType  所用的注解方法(暂时不需要修改,只支持InjectType.DEFAULT)
5.injectRule 使用方法 injectRule = @InjectRule(regex = "method+\\d*")

四.举一个例子
@Tracker
public class Server extends BaseServer {
    public Server() {

    }

    public String[] getSomeThings() {
        System.out.println("不只是这么简单");
        String[] tt = {"ddd", "sss"};
        return tt;
    }

    @Tracker(enable = false)
    public char getSomeThingsUnTracker() {
        return '1';
    }
}

这个例子中 getSomeThings()方法会得到注入,getSomeThingsUnTracker()方法则不会,(后续会添加配置规则功能)
得到的效果是:
public String[] getSomeThings() {
    if (TrackerDefaultInjector.isEnable("com.guuidea.tracker.Server", "getSomeThings", new String[0], new String[0], new Object[0])) {
        TrackerDefaultInjector.insertFront(this, "com.guuidea.tracker.Server", "getSomeThings", new String[0], new String[0], "", new Object[0]);
    }

    String[] tt = new String[]{"ddd", "sss"};
    if (TrackerDefaultInjector.isEnable("com.guuidea.tracker.Server", "getSomeThings", new String[0], new String[0], new Object[0])) {
        TrackerDefaultInjector.insertBack(this, "com.guuidea.tracker.Server", "getSomeThings", new String[0], new String[0], "", tt, new Object[0]);
    }
    return (String[])((String[])tt);
}

isEnable方法的参数说明：
1.类名
2.方法名
3.参数名
4.group
5.参数值

insertFront参数说明：
1.当前对象
2.类名
3.方法名
4.参数名
5.group
6.tag值
7.参数值

insertBack方法和insertFront参数说明方法的区别是
insertBack在倒数第二参数多了一个返回值，该例子上就多了一个tt，如果该方法有返回值则返回结果，没有则返回null


五.关于获取属性对象

 Command.instance().openGroup(group); //打开权限  只是打开权限 可以获取log
 Command.instance().addCommand("com.guuidea.tracker.Server.getSomeThings()", "before#getLog#");
  //在方法执行之前获取log
 Command.instance().addCommand("com.guuidea.tracker.Server.getSomeThings()", "after#getResult#[{\"name\":\"\",\"key\":1},{\"name\":\"aa\",\"key\":2}]");
 //在方法之后获取返回值的第一个元素，返回值本身是一个数组则name即空就可,该结果取的事返回值的第一个元素的aa属性的第二个元素
 Command.instance().addCommand("com.guuidea.tracker.Server.getSomeThings(a,b)", "before#getArgs#0#[{\"name\":\"\",\"key\":1}]");
 //方法执行前的第0个参数的第一个元素
 Command.instance().restoreCommand();

 目前获取对象数据的方式有一下四种
 public static final String GET_LOG = "getLog";  ////只支持在方法之前 即before#getLog
 //获取结果
 public static final String GET_RESULT = "getResult";  //只支持方法之后 即after#getResult
 //方法的参数
 public static final String GET_ARGS = "getArgs";  //方法前后都可以
 //类的属性
 public static final String GET_PROP = "getProp";  //方法前后都可以


6.关于配置规则
新增@InjectRule，它有一个isMainRule的属性，表示是否主rule，默认false就好

在app中新建一个类或者用在application上加一个注解如下：
@InjectRule(regex = "^test+\\w*", attrs = {Modifier.PUBLIC},isMainRule = true)
public class MainApplication extends Application {
}
这样表示匹配名字为test开头的方法名，且方法为PUBLIC属性，同时有效

除了主规则外,其他情况@InjectRule不可以单独使用，需配合@Tracker使用如下：
@Tracker(injectRule = @InjectRule(regex = "onCreate"))
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    }
}

regex:表示正则匹配
Modifier:目前支持的修饰符有
PUBLIC,PROTECTED,PRIVATE,ABSTRACT,STATIC,FINAL

如果同时存在主rule和当前rule，则以当前rule为准。

7.支持第匿名类(在正常类下的第一层匿名类),比如:
 1.mTvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
则插装路径可能需要根据获取getClass()方法来判断真实路径

注意：匿名类的方法插装需要在父类的基础上添加注解，本身不支持注解


8. 关于插装与否的判定，当前方法或类的颗粒度越小，权限越大，即
 @Tracker(injectRule = @InjectRule(regex = "test"))
 private class SearchClass {
    @Tracker()
    public class KeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
            return false;
        }
    }
 }

这个例子中KeyListener的注解@Tracker优先级高于SearchClass的注解，所以onKey方法还是会被插装

10.添加了一些工具类在TrackerUtils中，以及一些callback类中添加两个默认定义的addListener和UploadListener的方法处理分别为
RocketChatCallBack、RocketChatCallBackWithOutToken和DefaultUploadListenerImpl、RockerChatUploaderImpl


添加混淆规则
-keep public class com.sdk.tracker.Trackers
-keep public class com.sdk.tracker.log.TraceLog
-keep public class com.sdk.annotation.*{*;}
-keep public class com.sdk.tracker.listener.*{*;}


