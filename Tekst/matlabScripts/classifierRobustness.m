haarCCLAFp = [0;1;1;0;1;3;1;1;2;0;0;2];
lbpCCLAFp = [1;0;2;2;3;2;0;1;4;2;0;2];
hogCCLAFp = [1;0;1;0;1;1;0;2;1;2;1;1];
hogSVMFp = [1;0;0;0;0;0;0;0;0;0;0;0];

haarCCLACorr = [1;0;1;1;1;1;1;1;1;1;1;1];
lbpCCLACorr = [1;1;1;1;1;1;1;0;1;1;1;1];
hogCCLACorr = [0;0;1;1;1;1;1;1;1;0;1;1];
hogSVMCorr = [0;1;1;1;1;0;1;1;1;1;1;1];

x = 0:1:11;



fig = figure;

fig.Name = 'Feature robuustheid';
fig.NumberTitle = 'off';
fig.Position = [0,0,1000,400];

fp = subplot(1,2,1); % left subplot
corr = subplot(1,2,2); % right subplot
bar(fp, [haarCCLAFp lbpCCLAFp hogCCLAFp hogSVMFp])
set(fp,'YTick',0:1:4);
legend(fp,'HaarCCLA','LbpCCLA','HogCCLA','HogSVM');

% plot(fp,x,haarCCLAFp,x,lbpCCLAFp,x,hogCCLAFp,x,hogSVMFp)

xlabel(fp,'Testset')
ylabel(fp,'Valse positieven')

bar(corr, [haarCCLACorr lbpCCLACorr hogCCLACorr hogSVMCorr])
% plot(corr,x,haarCCLACorr,x,lbpCCLACorr,x,hogCCLACorr,x,hogSVMCorr)
% title(corr,'Feature robuustheid')
xlabel(corr,'Testset')
ylabel(corr,'Correcte resultaten')
set(corr,'YTick',0:1:4);
axis([fp corr],[0 13 0 5]);