oldCalc = zeros(2,2,7);
oldCalc(1,1,:) = [14.31 50.66 90.62 32.18 1.69 55.41 0];
oldCalc(1,2,:) = [0 0 0 0 0 0 261.73];

oldCalc(2,1,:) = [18.34 62.03 250.18 110.69 11.59 100.04 0];
oldCalc(2,2,:) = [0 0 0 0 0 0 566.54];

newCalc = zeros(2,2,5);
newCalc(1,1,:) = [15.42 36.68 160.16 566.03 0];
newCalc(1,2,:) = [0 0 0 0 780.77];

% newCalc(2,1,:) = [18.34 62.03 250.18 110.69 11.59 100.04 0];
% newCalc(2,2,:) = [0 0 0 0 0 0 566.54];

newCalcAA = zeros(2,2,5);
newCalcAA(1,1,:) = [15.42 36.68 160.16 566.03 0];
newCalcAA(1,2,:) = [0 0 0 0 780.77];

% newCalc(2,1,:) = [18.34 62.03 250.18 110.69 11.59 100.04 0];
% newCalc(2,2,:) = [0 0 0 0 0 0 566.54];


plotBarStackGroups(oldCalc, {'Simpel', 'Complex'});
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Contouren zoeken', 'Pose van blokken zoeken', 'Blokken toevoegen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

plotBarStackGroups(newCalc, {'Simpel', 'Complex'});
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Threshold berekenen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];