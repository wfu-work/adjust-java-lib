#!/usr/bin/env python3
"""Independent NumPy generalized least-squares oracle; no SDK/native code used."""
import json
from pathlib import Path
import sys
import numpy as np

points = np.array([[0., 0., 0.], [10., 2., .5], [4., 12., 1.], [-3., 8., 2.]])
edges = [(0, 1), (0, 2), (0, 3), (1, 2), (2, 3), (1, 3), (0, 1)]
base_cov = np.array([[.0001, .00002, .00001], [.00002, .000225, -.000015], [.00001, -.000015, .0004]])
A = np.zeros((len(edges)*3, 9)); P = np.zeros((len(edges)*3, len(edges)*3)); y = []
baselines = []
for i, (a, b) in enumerate(edges):
    # Fixed millimetre-scale offsets, comparable to the supplied covariance.
    # This is a designed regression fixture, not a random/field data sample.
    noise = np.array([((i*3)%7-3)*.008, ((i*5)%9-4)*.008, ((i*2)%5-2)*.012])
    obs = points[b]-points[a]+noise
    cov = base_cov*(1+i*.25)
    block = slice(i*3, i*3+3)
    for station, sign in [(a, -1), (b, 1)]:
        if station: A[block, (station-1)*3:station*3] = sign*np.eye(3)
    P[block, block] = np.linalg.inv(cov)
    y.extend(obs)
    baselines.append(dict(id=f'L{i+1}', **{'from':chr(65+a), 'to':chr(65+b)}, vector=dict(zip(['east','north','up'],obs)), covariance={'data':cov.ravel().tolist()}))
y = np.array(y); normal = A.T@P@A
x = np.linalg.solve(normal, A.T@P@y); residual = y-A@x
dof = len(y)-len(x); sigma0 = float(np.sqrt(residual@P@residual/dof)); covariance = np.linalg.inv(normal)*sigma0**2
fixture = {'problem':{'stations':[{'id':'A','fixed':True,'known_enu':{'east':0.,'north':0.,'up':0.}}]+[{'id':c} for c in 'BCD'], 'baselines':baselines},
           'generation':{'kind':'deterministic synthetic regression fixture', 'reference_points_m':points.tolist(), 'noise_formula_m':['((i*3)%7-3)*0.008','((i*5)%9-4)*0.008','((i*2)%5-2)*0.012'], 'covariance_scale':'1+i*0.25', 'index_origin':0},
           'oracle':{'method':'NumPy independent GLS: inv(A^T P A), covariance scaled by sigma0^2', 'numpy_version':np.__version__, 'coordinates':x.reshape(-1,3).tolist(), 'stddev':np.sqrt(np.diag(covariance)).reshape(-1,3).tolist(), 'residuals':residual.reshape(-1,3).tolist(), 'sigma0':sigma0, 'dof':dof, 'normal_condition':float(np.linalg.cond(normal))}}
Path(sys.argv[1]).write_text(json.dumps(fixture, ensure_ascii=False, indent=2)+'\n')
