#!/usr/local/bin/python3

#%% 
import matplotlib.pyplot as plt;
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

configTemplateFilenames = [
    "config-gossip-collect-query.txt",
    "config-plumtree-query.txt"
]

configs = {
    "seed" : [1],
    "cycle" : [50],
    "netsize" : [10],
    "kout": [5],
    "bc_msgnum" : [1],
    "bc_schedule" : ["0,20"],
    "qi_total" : [20],
    # "qo_logpath": dynamic log path
}



################################################################################
# global
################################################################################

cleanupList = []

# fig.1 the response hops with different network size and different degree

################################################################################
# analysis
################################################################################

# log analysis
def analyzeLog(path: str, config: dict):
    f = open(path)
    logs = []
    for jsonStr in f:
        if jsonStr == "":
            continue
        logs.append(json.load(jsonStr))

    analyzeLastLog(logs[-1])

def analyzeLastLog(jsonLog: dict, config: dict):
    # jsonLog["queryStats"][1]["stat"][]
    pass
# static

def printFinalHops(hops: list):
    fig = plt.figure()
    plt.title("回复跳数变化趋势")
    plt.xlabel("获取到的回复数")
    plt.ylabel("跳数")
    X = numpy.arange(1, 1+len(hops))
    plt.plot(X, hops, 'ro')
    plt.plot(X, hops, 'r')
    plt.ylim(0, max(hops)+1)
    plt.show()




################################################################################

# iterate different combinations by DFS
def DFS(confTplFilename: str, action: Callable):
    items = list(configs.items())
    l = len(items)
    indexs= [0]*l
    top = l
    while top > 0:
        top = l
        # do something with config and peersimConfig
        config = {}
        print(items)
        for i, (k, v) in enumerate(items):
            config[k] = v[indexs[i]]
        action(confTplFilename, config)

        # iterate the tree
        indexs[top-1] += 1
        while top > 0 and indexs[top-1] >= len(items[top-1][1]):
            indexs[top-1] = 0
            top -= 1
            if top > 0:
                indexs[top-1] += 1

def configAction(confTplFilename: str, config: dict):
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
    spr.run('java -cp `find -L lib/ -name "*.jar" | tr [:space:] :`:classes peersim.Simulator '+configName, shell=True)

    # analyze the log
    analyzeLog(path=logName, config=config)

    # choring
    cleanupList.append(logName)
    cleanupList.append(configName)

def cleanup():
    for file in cleanupList:
        spr.run('rm '+file, shell=True)


################################################################################
# main
################################################################################
# run all!
for confTplFilename in configTemplateFilenames:
    DFS(confTplFilename, configAction)

cleanup()