#!/bin/bash

# close all detached screen sessions (to instant close dummies)

screen -ls | grep Detached | cut -d. -f1 | awk '{print $1}' | xargs kill
