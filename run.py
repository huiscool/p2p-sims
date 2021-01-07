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

cleanupSet = set()

################################################################################
# run tasks
################################################################################

# iterate different combinations by DFS
# skipFilter skips some config when return true.
def DFS(configs: dict, skipFilter = (lambda conf: False)) ->list:
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
        if not skipFilter(param):
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

def compile():
    spr.run('rm -rf classes', shell=True)
    spr.run('mkdir -p classes', shell=True)
    spr.run('javac -sourcepath src -classpath `find -L lib/ -name "*.jar" | tr [:space:] :` -d classes `find -L . -name "*.java"`', shell=True)

def cleanup():
    for file in cleanupSet:
        spr.run('rm '+file, shell=True)

def run(configs: dict, skipFilter = (lambda conf: False))->list:
    return DFS(configs, skipFilter)


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

def runFig1():
    plt.figure(1)
    plt.title("回复跳数变化趋势")
    plt.xlabel("获取到的回复编号")
    plt.ylabel("跳数")

    params = run({
        "seed" : [99],
        "cycle" : [70],
        "netsize" : [10000],
        "kout": [5],
        "bc_msgnum" : [1],
        "bc_schedule" : ["0,20,40"],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
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
    plt.legend(handles=analyzeFigHopDist.legend)
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

def runFig2():
    plt.figure(2)
    plt.title("请求跳数分布")
    plt.xlabel("跳数")
    plt.ylabel("节点数")

    params = run({
        "seed" : [99],
        "cycle" : [70],
        "netsize" : [10000],
        "kout": [5],
        "bc_msgnum" : [1],
        "bc_schedule" : ["0,20,40"],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
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

def analyzeChurn(jsonLog: dict, index: int):
    plt.figure(3)
    hops = jsonLog["queryStats"][-1]["stat"]["arriveHops"]

    if not hops:
        print("no response for index "+str(index))
        return

    X = numpy.arange(0, len(hops))

    color = analyzeChurn.colors[index]
    plt.legend(handles=analyzeChurn.legend)
    plt.hist(hops,bins=10, color=analyzeChurn.colors[index], histtype='step')

    analyzeChurn.maxy = max(analyzeChurn.maxy, max(hops))
    analyzeChurn.maxx = max(analyzeChurn.maxx, len(hops))
    plt.ylim(0, analyzeChurn.maxy+5)
    plt.xlim(1, analyzeChurn.maxx+1)
    ax = plt.gca()
    ax.xaxis.set_major_locator(plt.MultipleLocator(1))
    ax.xaxis.set_major_formatter(plt.FormatStrFormatter('%2.0f'))
    return

# statics
analyzeChurn.colors = [
    'r', 'b', 'g', 'y',
    'r', 'b', 'g', 'y'
]
analyzeChurn.legend = [
    patches.Patch(color='r', label="0%"),
    patches.Patch(color='b', label="10%"),
    patches.Patch(color='g', label="20%"),
    patches.Patch(color='y', label="30%"),
    patches.Patch(color='r', label="0%"),
    patches.Patch(color='b', label="10%"),
    patches.Patch(color='g', label="20%"),
    patches.Patch(color='y', label="30%")
]
analyzeChurn.maxx = 0
analyzeChurn.maxy = 0

################################################################################

def runFig3():
    plt.figure(3)
    plt.title("不同故障率下的回复跳数分布")
    plt.xlabel("回复跳数")
    plt.ylabel("频数")

    params = run({
        "seed" : [94],
        "cycle" : [50],
        "netsize" : [10000],
        "kout": [5],
        "bc_msgnum" : [1],
        "bc_schedule" : ["5"],
        "churn_schedule": ["4"],
        "churn_percentage": ["0", "10", "20", "30"],
        "qi_total" : [40],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
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
        analyzeChurn(last, i)

################################################################################

def analyzeFailure(jsonLog: dict, index: int):
    plt.figure(4)
    queryNum, successNum = 0, 0 
    for msg in jsonLog["queryStats"][1:]: # skip the first broadcast
        queryNum += 1
        hops = msg["stat"]["arriveHops"]
        if not hops:
            # print("no hops")
            continue
        successNum += 1
    analyzeFailure.successRates.append(successNum/queryNum)
    
analyzeFailure.successRates = []

def runFig4():
    plt.figure(4)
    plt.title("节点数对回收成功率的影响")
    plt.xlabel("节点数")
    plt.ylabel("回收成功率")
    configs = {
        "seed" : [97],
        "cycle" : [600],
        "netsize" : list(range(20, 1000, 20)),
        "kout": [10],
        "bc_msgnum" : [1],
        "bc_schedule" : ["1," + ",".join(map(str,range(20,500,20))) ],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
        "qi_total" : [12],
        "conf_tpl": [
            "config-plumtree-query.txt"
        ],
        "strategy": [
            "random-period"
        ]
    }
    params = run(configs)

    for i,param in enumerate(params):
        f = open(param["logpath"])
        last = None
        for jsonStr in f:
            if jsonStr == "":
                continue
            last = json.loads(jsonStr)
        analyzeFailure(last, i)

    netsizes = configs["netsize"]

    plt.plot(netsizes, analyzeFailure.successRates, 'r')

################################################################################

def analyzeNonroot(jsonLog: dict, index: int):
    plt.figure(5)
    hops = jsonLog["queryStats"][-1]["stat"]["arriveHops"]

    if not hops:
        print("no hops for index "+str(index))
        return
    
    print(index, hops)
    plt.hist(hops,bins=10, color=analyzeNonroot.colors[index])

analyzeNonroot.colors = [
    'r', 'b'
]
analyzeNonroot.legend = [
    patches.Patch(color='r', label="根节点广播"),
    patches.Patch(color='b', label="非根节点广播")
]

# 比较根节点广播和非根节点广播的差异
def runFig5():
    plt.figure(5)
    plt.title("回复跳数分布图")
    plt.xlabel("跳数")
    plt.ylabel("频数")
    configs = {
        "seed" : [96],
        "cycle" : [40],
        "netsize" : [1000],
        "kout": [10],
        "bc_msgnum" : [1],
        "bc_schedule" : ["1,10"],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
        "qi_total" : [30],
        "conf_tpl": [
            "config-plumtree-query.txt"
        ],
        "strategy": [
            "fix-period",
            "random-period"
        ]
    }
    params = run(configs)
    for i,param in enumerate(params):
        f = open(param["logpath"])
        last = None
        for jsonStr in f:
            if jsonStr == "":
                continue
            last = json.loads(jsonStr)
        analyzeNonroot(last, i)
    
################################################################################
compile()

for task in [runFig3]:
    task()

plt.show()
cleanup()
# %%
