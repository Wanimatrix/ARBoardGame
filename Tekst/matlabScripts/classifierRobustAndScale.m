haarCCLAFp = [0;1;1;0;1;3;1;1;2;0;0;2]; %TODO: OLD
lbpCCLAFp = [1;0;2;2;4;2;1;1;3;0;0;1];
hogCCLAFp = [0;0;0;0;0;0;0;0;0;0;0;0];
hogSVMFp = [0;0;0;0;0;0;0;0;0;0;0;0];

haarCCLACorr = [1;0;1;1;1;1;1;1;1;1;1;1]; %TODO: OLD
lbpCCLACorr = [1;1;1;1;1;1;0;0;1;1;1;1];
hogCCLACorr = [0;0;1;1;0;1;1;1;1;1;1;1];
hogSVMCorr = [0;1;1;1;1;1;1;1;0;1;1;1];

scaleFCCLACorr = [10;10;9;7;5;4];
scaleFCCLAFp = [1;0;0;0;0;0];
scaleFCCLAPerf = [822.65;236.83;189.36;139.03;125.39;123.60];

scaleFSVMCorr = [11;10;10;8;7;6];
scaleFSVMFp = [1;1;0;0;0;0];
scaleFSVMPerf = [1315.64;396.64;244.94;164.33;125.58;110.87];

x = 0:1:11;
xScale = [1.01 1.05 1.1 1.15 1.2 1.25];



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

fig2 = figure;

fig2.Name = 'Scalefactor van classifier methodes';
fig2.NumberTitle = 'off';
fig2.Position = [0,0,1000,400];

corr2 = subplot(1,2,1); % left subplot
perf2 = subplot(1,2,2); % right subplot
plot(corr2, xScale, scaleFCCLACorr, xScale, scaleFSVMCorr);
% bar(fp, [haarCCLAFp lbpCCLAFp hogCCLAFp hogSVMFp])
set(corr2,'XTick',xScale);
set(corr2,'YTick',0:1:12);
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
set(perf2,'XTick',xScale);
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
axis([fp3 corr3],[0 13 0 2]);
legend(corr3,'HogCCLA','HogSVM');