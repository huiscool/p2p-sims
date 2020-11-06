#!/usr/local/bin/python3

#%% 
import matplotlib.pyplot as plt;
import matplotlib.patches as patches;
import numpy;
import string;
import os;
from collections.abc import Callable;
import subprocess as spr;
import re;
import json;

################################################################################
# config
################################################################################

plt.rcParams['font.sans-serif'] = 'Songti SC'

methods = [
    "plumtree",
    "dynamic tree"
]

globalConfigs = {
    "seed" : [1],
    "cycle" : [60],
    "netsize" : [10000],
    "kout": [5],
    "bc_msgnum" : [1],
    "bc_schedule" : ["0,20"],
    "qi_total" : [20],
    "conf_tpl": [
        "config-plumtree-query.txt",
        "config-gossip-collect-query.txt"
    ],
    # "qo_logpath": dynamic log path
}



################################################################################
# global
################################################################################

cleanupList = []

currentFig = 'figHop'
colors = 'bgrcmyk'

# fig.1 the response hops
figHop = plt.figure(1)
plt.title("回复跳数变化趋势")
plt.xlabel("获取到的回复数")
plt.ylabel("跳数")

################################################################################
# analysis
################################################################################

# log analysis
def analyzeLog(path: str, config: dict, configs: dict):
    f = open(path)
    logs = []
    for jsonStr in f:
        if jsonStr == "":
            continue
        logs.append(json.loads(jsonStr))

    analyzeLastLog(logs[-1], config, configs)

def analyzeLastLog(jsonLog: dict, config: dict, configs: dict):
    if currentFig == 'figHop':
        analyzeFigHop(jsonLog, config, configs)

def analyzeFigHop(jsonLog: dict, config: dict, configs: dict):
    plt.figure(1)
    lastLog = jsonLog["queryStats"][1]
    hops = lastLog["stat"]["arriveHops"]
    X = numpy.arange(1, 1+len(hops))
    color = colors[analyzeFigHop.index]

    plt.plot(X, hops, color+'o')
    plt.plot(X, hops, color)
    plt.ylim(0, max(hops)+5)
    plt.xlim(0, 1+len(hops))
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(1))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))
    
    analyzeFigHop.legend.append(patches.Patch(color=color, label=methods[analyzeFigHop.index]))
    analyzeFigHop.index += 1

analyzeFigHop.index = 0
analyzeFigHop.legend = []

################################################################################

# iterate different combinations by DFS
def DFS(configs: dict):
    items = list(configs.items())
    l = len(items)
    indexs= [0]*l
    top = l
    while top > 0:
        top = l
        # do something with config and peersimConfig
        config = {}
        for i, (k, v) in enumerate(items):
            config[k] = v[indexs[i]]
        configAction(config, configs)

        # iterate the tree
        indexs[top-1] += 1
        while top > 0 and indexs[top-1] >= len(items[top-1][1]):
            indexs[top-1] = 0
            top -= 1
            if top > 0:
                indexs[top-1] += 1

def configAction(config: dict, configs: dict):
    confTplFilename = config["conf_tpl"]
    # gen not repeated name for each task
    basename = os.path.splitext(confTplFilename)[0]
    joined = "_".join([basename] + list(map(str, config.values())))
    taskName = re.sub('[/.,*]', '_', joined)

    # exec template
    logName = taskName + ".log"
    configName = taskName + ".txt"

    config["qo_logpath"] = logName
    tpl = string.Template(open(confTplFilename, mode='r').read())
    out = tpl.substitute(config)

    conf = open(configName, mode='w')
    conf.write(out)
    conf.close()
    
    # exec task with current config
    spr.run('rm -rf classes', shell=True)
    spr.run('mkdir -p classes', shell=True)
    spr.run('javac -sourcepath src -classpath `find -L lib/ -name "*.jar" | tr [:space:] :` -d classes `find -L . -name "*.java"`', shell=True)
    spr.run('java -cp `find -L lib/ -name "*.jar" | tr [:space:] :`:classes peersim.Simulator '+configName, shell=True, stdout=spr.DEVNULL)

    # analyze the log
    analyzeLog(path=logName, config=config, configs=configs)

    # choring
    cleanupList.append(logName)
    cleanupList.append(configName)

def cleanup():
    for file in cleanupList:
        spr.run('rm '+file, shell=True)

def run(configs: dict):
    DFS(globalConfigs)
    cleanup()

################################################################################
# main
################################################################################

run(globalConfigs)
plt.legend(handles=analyzeFigHop.legend)
plt.show()