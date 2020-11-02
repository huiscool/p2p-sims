package main

import (
	"bufio"
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"sort"
	"strconv"
	"sync"
	"time"
)

var (
	count  = 5
	gap    = 10 * time.Second
	expNum = 10
	re     = regexp.MustCompile("^round-trip min/avg/max/stddev = ([0-9.]+)/([0-9.]+)/([0-9.]+)/([0-9.]+) ms$")
)

var (
	ips []string
)

func main() {
	iptable, err := os.Open("iptable1.txt")
	if err != nil {
		panic(err)
	}

	sc := bufio.NewScanner(iptable)
	for sc.Scan() {
		ip := sc.Text()
		if ip == "" {
			continue
		}
		ips = append(ips, ip)
	}
	for i := 0; i < expNum; i++ {
		exp(ips)
		if i != expNum-1 {
			time.Sleep(gap)
		}
	}
}

func exp(ips []string) {
	var wg sync.WaitGroup
	wg.Add(len(ips))
	var results singleResults
	var resultLock sync.Mutex
	for i, ip := range ips {
		go func(i int, ip string) {
			cmd := exec.Command("ping", ip, "-c "+strconv.Itoa(count))
			out, err := cmd.CombinedOutput()
			if err != nil {
				panic(err)
			}
			buf := bytes.NewBuffer(out)
			sc := bufio.NewScanner(buf)
			var avg float64
			for sc.Scan() {
				subs := re.FindStringSubmatch(sc.Text())
				if subs == nil {
					continue
				}
				avg, err = strconv.ParseFloat(subs[2], 8)
				if err != nil {
					panic(err)
				}
			}
			resultLock.Lock()
			results = append(results, singleResult{
				ip:     ip,
				num:    i,
				avgRTT: avg,
			})
			resultLock.Unlock()
			wg.Done()
		}(i, ip)
	}
	wg.Wait()

	// print a row
	sort.Sort(results)
	fmt.Println(results)
}

type singleResult struct {
	ip     string
	num    int
	avgRTT float64
}

type singleResults []singleResult

// Len is the number of elements in the collection.
func (rs singleResults) Len() int {
	return len(rs)
}

// Less reports whether the element with
// index i should sort before the element with index j.
func (rs singleResults) Less(i, j int) bool {
	return rs[i].avgRTT < rs[j].avgRTT
}

// Swap swaps the elements with indexes i and j.
func (rs singleResults) Swap(i, j int) {
	rs[i], rs[j] = rs[j], rs[i]
}

func (rs singleResults) String() string {
	out := ""
	for i := range rs {
		if i != 0 {
			out += ","
		}
		out += strconv.Itoa(rs[i].num)
	}
	return out
}
