# MicroBusAnnouncer
### 这是什么？

MicroBusAnnouncer是一款安卓公交报站器，集成了<b>语音播报</b>，<b>运行图展示</b>，<b>电显模拟</b>等功能。

<div>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/main.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/lines.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/stations.jpg" width="200"  alt=""/>
  <img src="https://github.com/Shiyue0x0/MicroBusAnnouncer/blob/master/readme/settings.jpg" width="200"  alt=""/>
</div>

### 快速开始

无需过多配置，只需要一些步骤即可体验Announcer的基本功能。
1. 准备一台Android 8.0+的手机，从下列渠道下载最新版，然后安装。

   [Github](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
   [蓝奏云](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
2. 下载体验语音库（桂林）。
   
   [Github](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)
   [蓝奏云](https://github.com/Shiyue0x0/MicroBusAnnouncer/releases)

3. 将其中的“Announcer”文件夹解压到手机的根目录。

4. 启动应用，并授予所需的权限。

5. 点击右下角的“设置”，划到“数据”，点击“加载预设<b>站点</b>数据”，此时应用会自动关闭。

6. 再次打开应用，点击“设置”-“数据”-“加载预设<b>路线</b>数据”，自动关闭后再打开应用。

7. 喜报！你已经完成了所有的初始设置。现在可以试着自行探索一下，或者是转到[开始运行](https://github.com/Shiyue0x0/MicroBusAnnouncer/tree/master?tab=readme-ov-file#开始运行)跟随文档操作。

### 开始运行

1. 现在，来试着运行路线。请先点击左下角的“主控”，接下来大部分的操作会在“主控”页进行。
   
2. 点击最上方的模拟电显，然后选择要运行的路线。也可以点击“线路”页中更换路线。
   
3. 此时，你选择的路线已经开始运行。Announcer在前台运行时会根据实时定位，在进站和出站时进行自动报站。
   
4. 如果你的定位没有改变，就无法触发自动报站。虽然但是，你仍可以点击界面下方中央类似铃铛的报站按钮，来试听报站。

5. 点击电显下方路线运行图任意一个站点，然后点击“当前站点”按钮，就会切换到这一站，再次点击报站按钮试听当前站点的报站。

### 语音播报

了解本章后，你可以定制属于自己的语音库。

语音库包含两大部分，播报格式和音频文件。

## Announcer的播报格式

Announcer的播报都是由若干个语句拼接而成的，而格式描述的是播报的内容和顺序。来看看下面这段格式。

`车辆启动|请站稳扶好|前方到站|<nscn>|下车乘客请准备`

每个语句用`|`分割开来，其中像`车辆启动`和`下车乘客请准备`这样的是普通语句。

而像`<nscn>`这样用一对尖括号包裹起来的内容，就是占位符语句。`<nscn>`代表的是*当前站点的中文名称*。

占位符主要有两类，一类是站点名称占位符。

站点名称占位符由两部分组成，第一部分表示站点在路线中的位置，第二部分表示站点的语种。

|    | cn         | en         | yue       |
| ns | 当前站点中文 | 当前站点英文 | 当前站点粤语 |
| ss | 起点站中文   | 起点站英文  | 起点站粤语 |
| ts | 终点站中文   | 终点站英文  | 终点站粤语 |

还有一些占位符，如下所示。

| <line> | 当前路线名称 |
| <time> | 当前时间 | 
| <hour> | 当前小时数 |
| <hour> | 当前分钟数 |
| <second> | 当前秒数 | 
| <speed> | 当前速度 | 

