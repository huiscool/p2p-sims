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

cleanupSet = set()

################################################################################
# run tasks
################################################################################

# iterate different combinations by DFS
def DFS(configs: dict) ->list:
    items = list(configs.items())
    l = len(items)
    indexs= [0]*l
    top = l
    params = []
    while top > 0:
        top = l
        # do something with config and peersimConfig
        config = {}
        for i, (k, v) in enumerate(items):
            config[k] = v[indexs[i]]
        param = configAction(config, configs)
        params.append(param)

        # iterate the tree
        indexs[top-1] += 1
        while top > 0 and indexs[top-1] >= len(items[top-1][1]):
            indexs[top-1] = 0
            top -= 1
            if top > 0:
                indexs[top-1] += 1
    return params

def configAction(config: dict, configs: dict) ->dict:
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

    # choring
    cleanupSet.add(logName)
    cleanupSet.add(configName)

    # return the parameters needed for analyzation
    return {
        "logpath": logName,
        "config": config,
        "configs": configs,
    }

def cleanup():
    for file in cleanupSet:
        spr.run('rm '+file, shell=True)

def run(configs: dict)->list:
    return DFS(configs)


################################################################################
# drawing
################################################################################

def analyzeFigRespHop(jsonLog: dict, index: int):
    plt.figure(1)
    hops = jsonLog["queryStats"][-1]["stat"]["arriveHops"]

    X = numpy.arange(1, 1+len(hops))

    color = analyzeFigRespHop.colors[index]
    plt.legend(handles=analyzeFigRespHop.legend)
    plt.plot(X, hops, color+'o')
    plt.plot(X, hops, color)

    if not hops:
        print("no response")
        return

    analyzeFigRespHop.maxy = max(analyzeFigRespHop.maxy, max(hops))
    plt.ylim(0, analyzeFigRespHop.maxy+5)
    plt.xlim(0, 1+len(hops))
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(10))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))
    return

# statics
analyzeFigRespHop.colors = [
    'r', 'b'
]
analyzeFigRespHop.legend = [
    patches.Patch(color='r', label="plumtree"),
    patches.Patch(color='b', label="dynamic tree")
]
analyzeFigRespHop.maxy = 0

################################################################################

plt.figure(1)
plt.title("回复跳数变化趋势")
plt.xlabel("获取到的回复编号")
plt.ylabel("跳数")

params = run({
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

for i,param in enumerate(params):
    f = open(param["logpath"])
    last = None
    for jsonStr in f:
        if jsonStr == "":
            continue
        last = json.loads(jsonStr)
    analyzeFigRespHop(last, i)
    

################################################################################

def analyzeFigHopDist(jsonLog: dict, index: int):
    plt.figure(2)
    hops = jsonLog["queryStats"][-1]["stat"]["reqHopCounter"]

    X = numpy.arange(0, len(hops))

    color = analyzeFigHopDist.colors[index]
    plt.legend(handles=analyzeFigRespHop.legend)
    plt.plot(X, hops, color+'o')
    plt.plot(X, hops, color)

    analyzeFigHopDist.maxy = max(analyzeFigHopDist.maxy, max(hops))
    analyzeFigHopDist.maxx = max(analyzeFigHopDist.maxx, len(hops))
    plt.ylim(0, analyzeFigHopDist.maxy+5)
    plt.xlim(0, analyzeFigHopDist.maxx)
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(1))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))
    return

# statics
analyzeFigHopDist.colors = [
    'r', 'b'
]
analyzeFigHopDist.legend = [
    patches.Patch(color='r', label="plumtree"),
    patches.Patch(color='b', label="dynamic tree")
]
analyzeFigHopDist.maxx = 0
analyzeFigHopDist.maxy = 0

################################################################################

figHopDist = plt.figure(2)
plt.title("请求跳数分布")
plt.xlabel("跳数")
plt.ylabel("节点数")

params = run({
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

for i,param in enumerate(params):
    f = open(param["logpath"])
    last = None
    for jsonStr in f:
        if jsonStr == "":
            continue
        last = json.loads(jsonStr)
    analyzeFigHopDist(last, i)

################################################################################

plt.show()
cleanup()
# %%
