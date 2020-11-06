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
    "seed" : [100],
    "cycle" : [60],
    "netsize" : [1000],
    "kout": [7],
    "bc_msgnum" : [1],
    "bc_schedule" : ["0,20"],
    "qi_total" : [20],
    "conf_tpl": [
        "config-plumtree-query.txt",
        "config-gossip-collect-query.txt"
    ],
    "strategy": [
        "fix-period",
        "random-period"
    ]
}

figs = [
    'figRespHop',
    'figHopDist'
]
colors = 'bgrcmyk'

################################################################################
# global
################################################################################

cleanupList = []

currentFig = figs[0]

# fig.1 the response hops
figRespHop = plt.figure(1)
plt.title("回复跳数变化趋势")
plt.xlabel("获取到的回复编号")
plt.ylabel("跳数")

figHopDist = plt.figure(2)
plt.title("请求跳数分布")
plt.xlabel("跳数")
plt.ylabel("节点数")

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
    if currentFig == 'figRespHop':
        analyzeFigRespHop(jsonLog, config, configs)
        return
    if currentFig == 'figHopDist':
        analyzeFigHopDist(jsonLog, config, configs)
        return

def analyzeFigRespHop(jsonLog: dict, config: dict, configs: dict):
    plt.figure(1)
    hops = jsonLog["queryStats"][-1]["stat"]["arriveHops"]

    X = numpy.arange(1, 1+len(hops))
    color = colors[analyzeFigRespHop.index]

    plt.plot(X, hops, color+'o')
    plt.plot(X, hops, color)

    if not hops:
        analyzeFigRespHop.legend.append(patches.Patch(color=color, label=methods[analyzeFigRespHop.index]))
        analyzeFigRespHop.index += 1
        print("no response")
        return


    analyzeFigRespHop.maxy = max(analyzeFigRespHop.maxy, max(hops))
    plt.ylim(0, analyzeFigRespHop.maxy+5)
    plt.xlim(0, 1+len(hops))
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(10))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))
    
    analyzeFigRespHop.legend.append(patches.Patch(color=color, label=methods[analyzeFigRespHop.index]))
    analyzeFigRespHop.index += 1

analyzeFigRespHop.index = 0
analyzeFigRespHop.legend = []
analyzeFigRespHop.maxy = 0

def analyzeFigHopDist(jsonLog: dict, config: dict, configs: dict):
    plt.figure(2)
    hops = jsonLog["queryStats"][-1]["stat"]["reqHopCounter"]
    print(hops)
    X = numpy.arange(0, len(hops))
    color = colors[analyzeFigHopDist.index]

    plt.plot(X, hops, color+'o')
    plt.plot(X, hops, color)

    analyzeFigHopDist.maxy = max(analyzeFigHopDist.maxy, max(hops))
    analyzeFigHopDist.maxx = max(analyzeFigHopDist.maxx, len(hops))
    plt.ylim(0, analyzeFigHopDist.maxy+5)
    plt.xlim(0, analyzeFigHopDist.maxx)
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(1))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))

    analyzeFigHopDist.legend.append(patches.Patch(color=color, label=methods[analyzeFigHopDist.index]))
    analyzeFigHopDist.index += 1

analyzeFigHopDist.index = 0
analyzeFigHopDist.legend = []
analyzeFigHopDist.maxx = 0
analyzeFigHopDist.maxy = 0

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
    taskName = re.sub('[/.,*-]', '_', joined)

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
    DFS(configs)
    cleanup()

################################################################################
# main
################################################################################

currentFig = figs[0]
run({
    "seed" : [99],
    "cycle" : [80],
    "netsize" : [1000],
    "kout": [5],
    "bc_msgnum" : [1],
    "bc_schedule" : ["0,20,40"],
    "qi_total" : [20],
    "conf_tpl": [
        "config-plumtree-query.txt",
        "config-gossip-collect-query.txt"
    ],
    "strategy": [
        "fix-period"
    ]
})
plt.legend(handles=analyzeFigRespHop.legend)

currentFig = figs[1]
run({
    "seed" : [99],
    "cycle" : [80],
    "netsize" : [1000],
    "kout": [5],
    "bc_msgnum" : [1],
    "bc_schedule" : ["0,20,40"],
    "qi_total" : [20],
    "conf_tpl": [
        "config-plumtree-query.txt",
        "config-gossip-collect-query.txt"
    ],
    "strategy": [
        "random-period"
    ]
})
plt.legend(handles=analyzeFigHopDist.legend)

plt.show()
# %%
