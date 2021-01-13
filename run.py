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

def rand():
    return random.randint(1, 99999999)

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
    config["seed"] = rand()
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
    spr.run('java -Xmx8g -cp `find -L lib/ -name "*.jar" | tr [:space:] :`:classes peersim.Simulator '+configName, shell=True, stdout=spr.DEVNULL)

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

def fig11():
    plt.figure(11)
    plt.title("节点出度-命中率关系图")
    plt.xlabel("节点出度")
    plt.ylabel("命中率")
    legend()

def fig12():
    plt.figure(12)
    plt.title("节点出度-接收消息数关系图")
    plt.xlabel("节点出度")
    plt.ylabel("接收消息数")
    legend()

def fig13():
    plt.figure(13)
    plt.title("节点出度-查询成功率关系图")
    plt.xlabel("节点出度")
    plt.ylabel("查询成功率")
    legend()

def fig14():
    plt.figure(14)
    plt.title("节点出度-平均回复跳数图")
    plt.xlabel("节点出度")
    plt.ylabel("回复跳数")
    legend()

def fig15():
    plt.figure(15)
    plt.title("节点出度-查询效率关系图")
    plt.xlabel("节点出度")
    plt.ylabel("查询效率")
    legend()

################################################################################

def reshapeAndGetMean(vecs, outputLen):
    if len(vecs) % outputLen != 0:
        raise Exception("cannot reshape")
    return numpy.asarray(vecs).reshape((-1, outputLen)).mean(axis=0)

def getEffs(hits, msgs, succs, hops):
    assert(len(hits)==len(msgs))
    assert(len(hits)==len(succs))
    assert(len(hits)==len(hops))
    l = len(hits)
    return [(hits[i]*succs[i])/(msgs[i]*hops[i]) for i in range(l)]

def analyze(param, hits, msgs, succs, hops):
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
        msg["stat"]["totalSendControl"] + 
        msg["stat"]["totalSendRequest"] + 
        msg["stat"]["totalSendResponse"] for msg in stats
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
    return

def runFig1to5():
    configs = {
        "seed": [rand() for i in range(5)],
        "cycle" : [1050],
        "netsize" : [10,20,50,100,200,500,1000,2000,5000,10000,20000,50000],
        "kout": [10],
        "bc_msgnum" : [1],
        "bc_schedule" : [",".join(map(str,range(5,1010,5)))],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
        "qi_total" : [12],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
        ],
        "strategy": [
            "random-period"
        ],
        "qo_beginning": [0]
    }

    netsizes = configs["netsize"]

    baselineParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-gossip-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(baselineParams):
        analyze(param, hits, msgs, succs, hops)
    hits = reshapeAndGetMean(hits, len(netsizes))
    msgs = reshapeAndGetMean(msgs, len(netsizes))
    succs = reshapeAndGetMean(succs, len(netsizes))
    hops = reshapeAndGetMean(hops, len(netsizes))
    effs = getEffs(hits,msgs, succs, hops)

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
        analyze(param, hits, msgs, succs, hops)
    
    hits = reshapeAndGetMean(hits, len(netsizes))
    msgs = reshapeAndGetMean(msgs, len(netsizes))
    succs = reshapeAndGetMean(succs, len(netsizes))
    hops = reshapeAndGetMean(hops, len(netsizes))
    effs = getEffs(hits,msgs, succs, hops)
    
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
        "seed": [rand() for i in range(1)],
        "cycle" : [1050],
        "netsize" : [10000],
        "kout": [10],
        "bc_msgnum" : [1],
        "bc_schedule" : [",".join(map(str,range(5,1000,5)))],
        "churn_schedule": ["1"],
        "churn_percentage": list(map(str, range(0, 1, 5))),
        "qi_total" : [12],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
        ],
        "strategy": [
            "random-period"
        ],
        "qo_beginning": [0]
    }

    percentages = [int(percentage) for percentage in configs["churn_percentage"]]

    baselineParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-gossip-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(baselineParams):
        analyze(param, hits, msgs, succs, hops)
    
    hits = reshapeAndGetMean(hits, len(percentages))
    msgs = reshapeAndGetMean(msgs, len(percentages))
    succs = reshapeAndGetMean(succs, len(percentages))
    hops = reshapeAndGetMean(hops, len(percentages))
    effs = getEffs(hits,msgs, succs, hops)
    
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
        analyze(param, hits, msgs, succs, hops)
    
    hits = reshapeAndGetMean(hits, len(percentages))
    msgs = reshapeAndGetMean(msgs, len(percentages))
    succs = reshapeAndGetMean(succs, len(percentages))
    hops = reshapeAndGetMean(hops, len(percentages))
    effs = getEffs(hits,msgs, succs, hops)

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

################################################################################

def runFig11to15():
    configs = {
        "seed": [rand() for i in range(1)],
        "cycle" : [1050],
        "netsize" : [10000],
        "kout": [5,10,20,50,100,200,500,1000],
        "bc_msgnum" : [1],
        "bc_schedule" : [",".join(map(str,range(5,1000,5)))],
        "churn_schedule": ["-1"],
        "churn_percentage": ["0"],
        "qi_total" : [12],
        "conf_tpl": [
            "config-gossip-collect-query.txt",
            "config-int-collect-query.txt"
        ],
        "strategy": [
            "random-period"
        ],
        "qo_beginning": [0]
    }

    degrees = configs["kout"]

    baselineParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-gossip-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(baselineParams):
        analyze(param, hits, msgs, succs, hops)
    
    hits = reshapeAndGetMean(hits, len(degrees))
    msgs = reshapeAndGetMean(msgs, len(degrees))
    succs = reshapeAndGetMean(succs, len(degrees))
    hops = reshapeAndGetMean(hops, len(degrees))
    effs = getEffs(hits,msgs, succs, hops)
    
    fig6()
    plt.plot(degrees, hits, color='r', linestyle="-", marker=".")
    fig7()
    plt.plot(degrees, msgs, color='r', linestyle="-", marker=".")
    fig8()
    plt.plot(degrees, succs, color='r', linestyle="-", marker=".")
    fig9()
    plt.plot(degrees, hops, color='r', linestyle="-", marker=".")
    fig10()
    plt.plot(degrees, effs, color='r', linestyle="-", marker=".")
    
    intbfsParams = run(configs, lambda param: param["config"]["conf_tpl"]=="config-int-collect-query.txt")
    hits = []
    msgs = []
    succs = []
    hops = []
    effs = []
    for i,param in enumerate(intbfsParams):
        analyze(param, hits, msgs, succs, hops)
    
    hits = reshapeAndGetMean(hits, len(degrees))
    msgs = reshapeAndGetMean(msgs, len(degrees))
    succs = reshapeAndGetMean(succs, len(degrees))
    hops = reshapeAndGetMean(hops, len(degrees))
    effs = getEffs(hits,msgs, succs, hops)

    fig6()
    plt.plot(degrees, hits, color='g', linestyle="-", marker=".")
    fig7()
    plt.plot(degrees, msgs, color='g', linestyle="-", marker=".")
    fig8()
    plt.plot(degrees, succs, color='g', linestyle="-", marker=".")
    fig9()
    plt.plot(degrees, hops, color='g', linestyle="-", marker=".")
    fig10()
    plt.plot(degrees, effs, color='g', linestyle="-", marker=".")

compile()

for task in [
    # runFig1to5,
    # runFig6to10,
    runFig11to15,
    ]:
    task()

plt.show()
cleanup()
# %%
