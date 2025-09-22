# MicroBusAnnouncer

### 这是什么？

MicroBusAnnouncer是一款安卓公交报站器，集成了<b>语音播报</b>，<b>运行图展示</b>，<b>电显模拟</b>等功能。

<div>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/img/main.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/img/lines.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/img/stations.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/img/settings.jpg" width="200"  alt=""/>
</div>

### 快速开始

无需过多配置，只需要一些步骤即可体验Announcer的基本功能。

1. 准备一台Android 8.0+的手机，从下列渠道下载最新版，然后安装。

   [Github](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
   [蓝奏云](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
2. 下载体验语音库（桂林）。

   [Github](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
   [蓝奏云](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)

3. 将其中的`Announcer`文件夹解压到手机的根目录。

4. 启动应用，并授予所需的权限。

5. 点击右下角的`设置`-`数据与关于`，点击`加载预设站点数据`，此时应用会自动关闭。

6. 再次打开应用，点击`设置`-`数据与关于`-`加载预设路线数据`，自动关闭后再打开应用。


喜报！你已经完成了所有的初始设置。现在可以试着自行探索一下，或者是转到[开始运行](https://github.com/Shiyue0x0/MicroBusAnnouncer/tree/master?tab=readme-ov-file#开始运行)
跟随文档操作。

### 开始运行

1. 现在，来试着运行路线。请先点击左下角的`主控`，接下来大部分的操作会在`主控`页进行。

2. 点击最上方的模拟电显，然后选择要运行的路线。也可以点击`线路`页中更换路线。

3. 此时，你选择的路线已经开始运行。Announcer在前台运行时会根据实时定位，在进站和出站时进行自动报站。

4. 如果你的定位没有改变，就无法触发自动报站。虽然但是，你仍可以点击界面下方中央类似铃铛的报站按钮，来试听报站。

5. 点击电显下方路线运行图任意一个站点，然后点击`当前站点`按钮，就会切换到这一站，再次点击报站按钮试听当前站点的报站。

### 语音播报

了解本章后，你可以定制属于自己的语音库。

语音库包含两大部分，播报格式和音频文件。

#### Announcer的播报格式

Announcer的播报都是由若干个语句拼接而成的，而格式描述的是播报的内容和顺序。来看看下面这段格式。

`车辆启动|请站稳扶好|前方到站|<nscn>|下车乘客请准备`

每个语句用`|`分割开来，其中像`车辆启动`和`下车乘客请准备`这样的是普通语句。

而像`<nscn>`这样用一对尖括号包裹起来的内容，就是占位符语句。`<nscn>`代表的是*当前站点的中文名称*。

占位符语句主要有两类，一类是`站点名称占位符`。

站点名称占位符由两部分组成，第一部分表示站点在路线中的位置，第二部分表示站点的语种，如下表所示。

|    | cn     | en     | yue    |
|----|--------|--------|--------|
| ns | 当前站点中文 | 当前站点英文 | 当前站点粤语 |
| ss | 起点站中文  | 起点站英文  | 起点站粤语  |
| ts | 终点站中文  | 终点站英文  | 终点站粤语  |

其中，中文和英文是必须的，此外，你也可以自定义语种，例如表中的粤语。

注意：站点名称占位符要求顺序一致。如果要表示`终点站粤语`，不能使用`<yuets>`，正确用法是`<tsyue>`。

还有一些`其他占位符`，如下表所示。

| 占位符        | 含义     |
|------------|--------|
| `<line>`   | 当前路线名称 |
| `<time>`   | 当前时间   |
| `<hour>`   | 当前小时数  |
| `<hour>`   | 当前分钟数  |
| `<second>` | 当前秒数   |
| `<speed>`  | 当前速度   |

在`设置`-`语音播报库`中，可以自定义播报格式。

#### Announcer的音频文件

Announcer会按照播报格式在语音库中寻找对应的语音。

1. 对于常规语句，Announcer会在`Announcer/Media/[语音库名称]`下所有语种的`common`文件夹中查找文件名为语句文本的音频文件。

2. 对于站点占位符语句，Announcer会在`Announcer/Media/[语音库名称]/[语种]/station`中查找文件名为站点名称的音频文件。

3. 对于其他占位符语句，Announcer按照特定的规则处理。

来看看下面这段格式。

`<nscn>|到了|请从后门下车|We are arriving at|<nsen>`

假设当前站点为`桂林北站(Guilin North Station)`，语音库为`MicroBus`

1. 对于`到了`，Announcer会在`Announcer/Media/MicroBus/cn/common` `Announcer/Media/MicroBus/en/common`
   等文件夹中查找文件名为`到了`的音频文件并播报。（如果存在对应文件）

2. 对于`<nsen>`，Announcer会在`Announcer/Media/MicroBus/en/station`中查找文件名为
   `Guilin North Station`的音频文件。 此时文件夹中如果存在 `Guilin North Station.wav`
   `Guilin North Station.mp3` `Guilin North Station.ogg`
   或者其他格式的音频文件，Announcer就会将其读取并播报。

尽管对于常规语句，Announcer会不区分语种地进行查找，但还是建议您按语种存放音频文件。

如果找不到对应的音频文件，且您启用了`TTS播报`，Announcer会调用系统的`文字转语音服务`进行播报。

### 自定义站点与路线

你可以从云端获取路线，也可以自行添加站点和路线。

`站点`或者`路线`选项卡中罗列了所有的本地`站点`或`路线`。

要添加`站点`或`路线`，请点击屏幕下方的`加号按钮`。

#### 自定义站点

要添加站点，更便捷的操作是前往`设置`-`地图与定位`开启`单击地图添加站点`，然后回到`主控`。
在地图上点击要添加的站点的位置，Announcer会自动填入经纬度，您只需要输入其他信息即可完成添加。

#### 自定义路线

1. 在地图上选择站点录入（3.0 版本起）

2. 通过站点ID录入

#### 数据库

Announcer采用`SQLite`数据库存储`站点`和`路线`，如有需要，您可以直接编辑数据库文件。

1. 前往`设置`-`数据与关于`，点击`备份站点与路线`。

2. 在`Announcer/Backups`文件夹中找到刚刚备份的`station.db`或`line.db`并提取，现在请按自己的方式编辑数据库文件。

3. 前往`设置`-`数据与关于`，点击`还原站点`或者`还原路线`，选择编辑好的文件。

