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
import random;
import copy;

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
def DFS(configs: dict, filter = (lambda param: True)) ->list:
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
        param = genParam(config, configs)
        if filter(param):
            params.append(param)
            execConfig(param["configname"], param["logname"])

        # iterate the tree
        indexs[top-1] += 1
        while top > 0 and indexs[top-1] >= len(items[top-1][1]):
            indexs[top-1] = 0
            top -= 1
            if top > 0:
                indexs[top-1] += 1
    return params

def genParam(config: dict, configs: dict) ->dict:
    config["seed"] = random.randint(1, 1000)
    confTplFilename = config["conf_tpl"]
    # gen not repeated name for each task
    basename = os.path.splitext(confTplFilename)[0]
    configWithoutBC = copy.deepcopy(config)
    del configWithoutBC["bc_schedule"]
    joined = "_".join([basename] + list(map(str, configWithoutBC.values())))
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

    cleanupSet.add(configName)

    # return the parameters needed for analyzation
    return {
        "logname": logName,
        "configname": configName,
        "config": config,
        "configs": configs,
    }

def execConfig(configName, logName):
    spr.run('java -cp `find -L lib/ -name "*.jar" | tr [:space:] :`:classes peersim.Simulator '+configName, shell=True, stdout=spr.DEVNULL)

    # choring
    cleanupSet.add(logName)

def compile():
    spr.run('rm -rf classes', shell=True)
    spr.run('mkdir -p classes', shell=True)
    spr.run('javac -sourcepath src -classpath `find -L lib/ -name "*.jar" | tr [:space:] :` -d classes `find -L . -name "*.java"`', shell=True)

def cleanup():
    for file in cleanupSet:
        spr.run('rm '+file, shell=True)

def run(configs: dict, filter = (lambda param: True))->list:
    return DFS(configs, filter)


################################################################################
# drawing
################################################################################

def legend():
    plt.legend(handles=[
        patches.Patch(color='r', label="baseline"),
        patches.Patch(color='g', label="intbfs")
    ])

def fig1():
    plt.figure(1)
    plt.title("节点数-命中率关系图")
    plt.xlabel("节点数")
    plt.ylabel("命中率")
    legend()

def fig2():
    plt.figure(2)
    plt.title("节点数-接收消息数关系图")
    plt.xlabel("节点数")
    plt.ylabel("接收消息数")
    legend()

def fig3():
    plt.figure(3)
    plt.title("节点数-查询成功率关系图")
    plt.xlabel("节点数")
    plt.ylabel("查询成功率")
    legend()

def fig4():
    plt.figure(4)
    plt.title("节点数-平均回复跳数关系图")
    plt.xlabel("节点数")
    plt.ylabel("回复跳数")
    legend()

def fig5():
    plt.figure(5)
    plt.title("节点数-查询效率关系图")
    plt.xlabel("节点数")
    plt.ylabel("查询效率")
    legend()

################################################################################

def fig6():
    plt.figure(6)
    plt.title("故障率-命中率关系图")
    plt.xlabel("故障率")
    plt.ylabel("命中率")
    legend()

def fig7():
    plt.figure(7)
    plt.title("故障率-接收消息数关系图")
    plt.xlabel("故障率")
    plt.ylabel("接收消息数")
    legend()

def fig8():
    plt.figure(8)
    plt.title("故障率-查询成功率关系图")
    plt.xlabel("故障率")
    plt.ylabel("查询成功率")
    legend()

def fig9():
    plt.figure(9)
    plt.title("故障率-平均回复跳数图")
    plt.xlabel("故障率")
    plt.ylabel("回复跳数")
    legend()

def fig10():
    plt.figure(10)
    plt.title("故障率-查询效率关系图")
    plt.xlabel("故障率")
    plt.ylabel("查询效率")
    legend()

################################################################################

def analyze(param, hits, msgs, succs, hops, effs):
    f = open(param["logname"])
    last = None
    for jsonStr in f:
        if jsonStr == "":
            continue
    last = json.loads(jsonStr)
    stats = last["queryStats"]
    # how many requests
    reqnum = len(stats)
    # hits
    dmn = param["config"]["qi_total"] * reqnum
    mnr = sum([msg["stat"]["hits"] for msg in stats])
    hit = mnr/dmn
    hits.append(hit)
    # msgs
    dmn = param["config"]["netsize"] * reqnum
    mnr = sum(
        [
        msg["stat"]["totalRecvControl"] + 
        msg["stat"]["totalRecvRequest"] + 
        msg["stat"]["totalRecvResponse"] for msg in stats
        ])
    msg = mnr/dmn
    msgs.append(msg)
    # succs
    dmn = reqnum
    mnr = sum([1 if msg["stat"]["arriveHops"] else 0 for msg in stats])
    succ = mnr/dmn
    succs.append(succ)
    # hops
    dmn = sum([1 if msg["stat"]["arriveHops"] else 0 for msg in stats])
    mnr = sum([numpy.mean(msg["stat"]["arriveHops"]) if msg["stat"]["arriveHops"] else 0 for msg in stats])
    hop = mnr/dmn
    hops.append(hop)
    # effs
    effs.append((hit/msg)*(succ/hop))
    return

def runFig1to5():
    configs = {
        "cycle" : [600],
        "netsize" : [10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,100000],
        "kout": [10],
        "bc_msgnum" : [1],
        "bc_schedule" : ["1," + ",".join(map(str,range(10,550,10))) ],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
        "qi_total" : [12],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
        ],
        "strategy": [
            "random-period"
        ]
    }

    netsizes = configs["netsize"]

    baselineParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-gossip-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(baselineParams):
        analyze(param, hits, msgs, succs, hops, effs)
    
    fig1()
    plt.plot(netsizes, hits, color='r', linestyle="-", marker=".")
    fig2()
    plt.plot(netsizes, msgs, color='r', linestyle="-", marker=".")
    fig3()
    plt.plot(netsizes, succs, color='r', linestyle="-", marker=".")
    fig4()
    plt.plot(netsizes, hops, color='r', linestyle="-", marker=".")
    fig5()
    plt.plot(netsizes, effs, color='r', linestyle="-", marker=".")
    
    intbfsParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-int-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(intbfsParams):
        analyze(param, hits, msgs, succs, hops, effs)
    
    fig1()
    plt.plot(netsizes, hits, color='g', linestyle="-", marker=".")
    fig2()
    plt.plot(netsizes, msgs, color='g', linestyle="-", marker=".")
    fig3()
    plt.plot(netsizes, succs, color='g', linestyle="-", marker=".")
    fig4()
    plt.plot(netsizes, hops, color='g', linestyle="-", marker=".")
    fig5()
    plt.plot(netsizes, effs, color='g', linestyle="-", marker=".")

################################################################################

def runFig6to10():
    configs = {
        "cycle" : [600],
        "netsize" : [10000],
        "kout": [20],
        "bc_msgnum" : [1],
        "bc_schedule" : [",".join(map(str,range(10,500,10))) ],
        "churn_schedule": ["1"],
        "churn_percentage": ["0", "5", "10", "15", "20", "25", "30"],
        "qi_total" : [12],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
        ],
        "strategy": [
            "random-period"
        ]
    }

    percentages = [int(percentage) for percentage in configs["churn_percentage"]]

    baselineParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-gossip-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(baselineParams):
        analyze(param, hits, msgs, succs, hops, effs)
    
    fig6()
    plt.plot(percentages, hits, color='r', linestyle="-", marker=".")
    fig7()
    plt.plot(percentages, msgs, color='r', linestyle="-", marker=".")
    fig8()
    plt.plot(percentages, succs, color='r', linestyle="-", marker=".")
    fig9()
    plt.plot(percentages, hops, color='r', linestyle="-", marker=".")
    fig10()
    plt.plot(percentages, effs, color='r', linestyle="-", marker=".")
    
    intbfsParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-int-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(intbfsParams):
        analyze(param, hits, msgs, succs, hops, effs)
    print(hits, msgs, succs, hops, effs)
    fig6()
    plt.plot(percentages, hits, color='g', linestyle="-", marker=".")
    fig7()
    plt.plot(percentages, msgs, color='g', linestyle="-", marker=".")
    fig8()
    plt.plot(percentages, succs, color='g', linestyle="-", marker=".")
    fig9()
    plt.plot(percentages, hops, color='g', linestyle="-", marker=".")
    fig10()
    plt.plot(percentages, effs, color='g', linestyle="-", marker=".")

compile()

for task in [runFig1to5]:
    task()

plt.show()
cleanup()
# %%
