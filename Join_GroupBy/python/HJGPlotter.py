'''
Created on May 5, 2015

@author: ak
'''
import matplotlib
#matplotlib.use('Agg')
import time
import sys
from itertools import izip_longest
import pylab as pl
from matplotlib import rc
from matplotlib import cm
from mpl_toolkits.mplot3d.axes3d import Axes3D
from mpl_toolkits.axisartist.axis_artist import Ticks
from scipy.io.matlab.mio5_utils import scipy

def grouper(n, iterable, fillvalue=None):
    "grouper(3, 'ABCDEFG', 'x') --> ABC DEF Gxx"
    args = [iter(iterable)] * n
    return izip_longest(fillvalue=fillvalue, *args)


#plotting settings
rc('font', **{'family': 'serif', 'serif': ['Computer Modern']})
rc('text', usetex=True)

def multiplePlots2d(plotRanges, data, 
                    labels=None, 
                    yScale='linear',
                    styles=None,
                    xticks=None,
                    xLabel=None,
                    yLabel=None,
                    title=None, 
                    grid=True,
                    saveFlag=False, 
                    filename=None,
                    showFlag=True):
    '''
    function multiplePlots2d:
    creates a 2d plot of many data arrays
    args:
        @param plotRanges: array of ranges for each data array to plot
        @param data: 2d array of data, array of data arrays to plot
        @param labels: array of labels for each data element to plot
        @param yScale: plotting scale 'linear' or 'log'
        @param styles: array of line styles
        @param xticks: ticks of x axis
        @param xLabel: label of x axis
        @param yLabel: label of y axis
        @param title: plot title
        @param grid: plot grid
        @param saveFlag: (boolean) save figure
        @param filename: filename to save under (no .ext required)
        @param showFlag: (boolean) show figure
    '''
    if not any((isinstance(k,list) or isinstance(k,pl.ndarray)) for k in plotRanges):
        plotRanges=[plotRanges]
    if not any((isinstance(k,list) or isinstance(k,pl.ndarray)) for k in data):
        data=[data]
    if labels and (not isinstance(labels, list)):
        labels=[labels]
    if styles and (not isinstance(styles, list)):
        styles=[styles]
        
    fig,axes=pl.subplots(figsize=(9,7))
    for i in range(len(data)):
        if styles:
            axes.plot(plotRanges[i],data[i],styles[i],label=(labels[i] if labels else None))
        else:
            axes.plot(plotRanges[i],data[i],label=(labels[i] if labels else None))
    axes.legend()
    axes.grid(grid)
    axes.set_xlim([min(i[0] for i in plotRanges), max(i[-1] for i in plotRanges)])
    axes.set_xlabel(xLabel)
    if xticks!=None:
        axes.set_xticks(xticks)
    axes.set_ylabel(yLabel)
    axes.set_yscale(yScale) #for log use yScale='log'
    axes.set_title(title)
    fig.tight_layout()
    if saveFlag:
        if filename:
            fig.savefig(filename+'.png')
        else:
            print('No filename specified,not saving')
    if showFlag:
        fig.show()
        time.sleep(5)
    pl.close()
    
    
if __name__ == '__main__':
    
    #correct usage
    if len(sys.argv)!=2:
        print("correct usage: python HJGPlotter.py filename")
        sys.exit()
    
    with open(sys.argv[1],"r") as f:
        lines=f.readlines()
        data={}
        for i in range(len(lines)/4):
            data[lines[i*4].strip().split(" ",1)[1]]={"reducers":[],"real":[],"user":[],"sys":[]}
        print(data.keys())
        for desc,real,user,syst in grouper(4,lines):
            jg=desc.strip().split(" ",1)[1]
            print(jg)
            r=desc.strip().split(" ",1)[0]
            print(r)
            data[jg]["reducers"].append(float(r))
            data[jg]["real"].append(float(real.strip().split()[1]))
            data[jg]["user"].append(float(user.strip().split()[1]))
            data[jg]["sys"].append(float(syst.strip().split()[1]))
        
        for k in data.keys():
            multiplePlots2d([data[k]["reducers"]]*4,[data[k]["real"],data[k]["user"],data[k]["sys"]],
                            labels=["real","user","sys"],
                            xLabel="reducers",
                            yLabel="time",
                            yScale="log",
                            xticks=data[k]["reducers"],
                            title="join:"+k.split()[0]+" group:"+k.split()[1],
                            saveFlag=False,
                            showFlag=True,
                            filename="_".join(k.split())
                            )
            