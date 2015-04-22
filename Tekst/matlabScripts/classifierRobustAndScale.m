haarCCLAFp = [1;0;0;0;0;0;0;0;1;0;0;0;0;0;0;0;0;0;0;0]; 
lbpCCLAFp = [6;4;7;6;11;8;5;5;8;3;4;7;5;5;13;5;9;1;7;7];
hogCCLAFp = [0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0];
hogSVMFp = [0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0];

haarCCLACorr = [1;1;0;1;0;1;1;1;0;1;1;1;1;1;1;1;1;1;1;1]; 
lbpCCLACorr = [1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1];
hogCCLACorr = [0;1;1;1;0;1;1;1;0;1;1;1;1;1;1;1;0;1;1;1];
hogSVMCorr = [1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1;1];

haarCCLAPerf = [139.773;124.956;117.539;118.822;123.658;132.379;117.759;122.112;122.245;119.972;122.181;123.159;110.176;114.506;134.213;140.798;167.57;114.923;127.788;131.792];
lbpCCLAPerf = [96.2277;87.1832;87.1803;88.7729;91.9265;99.5962;82.531;86.9067;91.6596;84.0861;74.4071;84.4947;82.3185;87.9893;101.866;98.2576;103.706;63.0058;85.7013;100.01];
hogCCLAPerf = [163.456;158.918;162.13;162.342;164.862;165.368;163.316;168.88;160.056;163.741;163.701;164.713;166.18;160.968;163.07;168.507;164.085;162.75;164.15;161.329];
hogSVMPerf = [181.266;190.11;196.28;183.996;181.44;180.044;185.04;180.334;182.712;187.265;181.077;180.156;179.424;179.957;185.39;181.8;182.879;183.449;179.567;181.47];

scaleFCCLACorr = [20;16;11;11;7;8;0;2;2;0;2;1;0;0];
% scaleFCCLAFp = [1;0;0;0;0;0];
scaleFCCLAPerf = [797.475;183.142;99.841;78.4038;63.916;60.9719;39.3613;39.334;38.8128;45.3292;44.3135;42.8134;24.5078;24.381;24.7649;24.1793;25.0387;26.6454;26.7158;24.9017;24.429;24.4015;24.4662;24.5102;24.57;24.5745;24.6986;24.1608;26.4312;24.8266;24.3147;24.7226;24.4198;24.6179;24.5247;24.0797;24.2977;24.5328;25.8775;25.6471;25.0051;25.1057;24.223;24.1855;24.7887;24.269;24.0902;23.9963;23.8589;24.1696;24.308;24.5123;24.7802;24.1881;23.8487;24.9205;24.0582;23.9982;23.9586;24.2981;24.3336];

scaleFSVMCorr = [20;20;15;13;8;4;2;1;0;1;0;0;0;0];
% scaleFSVMFp = [1;1;0;0;0;0];
scaleFSVMPerf = [823.085;291;127.708;90.0268;73.3768;63.7008;55.1457;49.5616;47.376;45.2944;43.1037;41.7256;41.2233;40.4293;39.3798;38.9711;37.999;38.2075;39.1325;38.2405;37.5298;36.7745;36.8173;37.0843;37.9235;36.2323;36.5762;36.1464;36.1237;36.3095;36.6638;37.0478;36.2681;35.9555;36.5026;36.105;39.1857;36.5755;37.872;35.5157;35.9123;36.6146;36.2139;35.6437;36.3779;35.5803;35.9627;35.7776;35.7628;35.6281;35.7327;36.0139;36.2869;35.8236;35.9476;37.6811;38.3105;35.8514;35.5594;36.1619;35.7159];

x = 0:1:11;
xScale = [1.01 1.05:0.05:4];
xScaleCorr = [1.01 1.05:0.05:1.65];



fig = figure;

fig.Name = 'Feature robuustheid';
fig.NumberTitle = 'off';
fig.Position = [0,0,1000,400];

fp = subplot(1,2,1); % left subplot
corr = subplot(1,2,2); % right subplot
bar(fp, [haarCCLAFp lbpCCLAFp hogCCLAFp])
set(fp,'YTick',0:1:4);
legend(fp,'HaarCCLA','LbpCCLA','HogCCLA');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel(fp,'Testset')
ylabel(fp,'Valse positieven')

bar(corr, [haarCCLACorr lbpCCLACorr hogCCLACorr])
% plot(corr,x,haarCCLACorr,x,lbpCCLACorr,x,hogCCLACorr,x,hogSVMCorr)
% title(corr,'Feature robuustheid')
xlabel(corr,'Testset')
ylabel(corr,'Correcte resultaten')
set(corr,'YTick',0:1:4);
axis([fp corr],[0 13 0 5]);
axis(fp,[0 21 0 14]);
axis(corr,[0 21 0 2]);

fig2 = figure;

fig2.Name = 'Scalefactor van classifier methodes';
fig2.NumberTitle = 'off';
fig2.Position = [0,0,1000,400];

corr2 = subplot(1,2,1); % left subplot
perf2 = subplot(1,2,2); % right subplot
plot(corr2, xScaleCorr, scaleFCCLACorr, xScaleCorr, scaleFSVMCorr);
% bar(fp, [haarCCLAFp lbpCCLAFp hogCCLAFp hogSVMFp])
set(corr2,'XTick',1:0.2:1.7);
set(corr2,'YTick',0:1:21);
legend(corr2,'CCLA','SVM');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel(corr2,'Scalefactor')
ylabel(corr2,'Aantal correcte resultaten')

plot(perf2, xScale, scaleFCCLAPerf, xScale, scaleFSVMPerf);
% bar(corr, [haarCCLACorr lbpCCLACorr hogCCLACorr hogSVMCorr])
% plot(corr,x,haarCCLACorr,x,lbpCCLACorr,x,hogCCLACorr,x,hogSVMCorr)
% title(corr,'Feature robuustheid')
xlabel(perf2,'Scalefactor')
ylabel(perf2,'Performantie (ms)')
set(perf2,'XTick',1:0.5:4);
set(perf2,'YTick',0:100:1500);
legend(perf2,'CCLA','SVM');
% axis([fp corr],[0 13 0 5]);


fig3 = figure;

fig3.Name = 'Robuustheid classificatie methodes';
fig3.NumberTitle = 'off';
fig3.Position = [0,0,1000,400];

fp3 = subplot(1,2,1); % left subplot
corr3 = subplot(1,2,2); % right subplot
bar(fp3, [hogCCLAFp hogSVMFp])
set(fp3,'YTick',0:1:4);
legend(fp3,'HogCCLA','HogSVM');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel(fp3,'Testset')
ylabel(fp3,'Valse positieven')

bar(corr3, [hogCCLACorr hogSVMCorr])
% plot(corr,x,haarCCLACorr,x,lbpCCLACorr,x,hogCCLACorr,x,hogSVMCorr)
% title(corr,'Feature robuustheid')
xlabel(corr3,'Testset')
ylabel(corr3,'Correcte resultaten')
set(corr3,'YTick',0:1:4);
axis(fp3,[0 21 0 14]);
axis(corr3,[0 21 0 2]);
legend(corr3,'HogCCLA','HogSVM');

fig4 = figure;

fig4.Name = 'Feature performantie';
fig4.NumberTitle = 'off';
fig4.Position = [0,0,800,400];

bar([haarCCLAPerf lbpCCLAPerf hogCCLAPerf])
% set(fp,'YTick',0:1:4);
legend('HaarCCLA','LbpCCLA','HogCCLA');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel('Testset')
ylabel('Performantie (ms)')

fig5 = figure;

fig5.Name = 'Classifier performantie';
fig5.NumberTitle = 'off';
fig5.Position = [0,0,800,400];

bar([hogCCLAPerf hogSVMPerf])
% set(fp,'YTick',0:1:4);
legend('HogCCLA','HogSVM');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel('Testset')
ylabel('Performantie (ms)')