oldCalc = zeros(2,2,7);
% Simpel
oldCalc(1,1,:) = [10.35 29.67 35.30 17.01 1.06 8.55 0];
oldCalc(1,2,:) = [0 0 0 0 0 0 112.80];
% Complex
oldCalc(2,1,:) = [13.11 38.21 40.96 161.50 7.96 35.98 0];
oldCalc(2,2,:) = [0 0 0 0 0 0 310.16];

% Simpel
newCalc = zeros(2,2,5);
newCalc(1,1,:) = [12.63 31.31 131.18 240.49 0];
newCalc(1,2,:) = [0 0 0 0 422.41];
% Complex
newCalc(2,1,:) = [15.58 42.23 179.32 245.49 0];
newCalc(2,2,:) = [0 0 0 0 491.91];

% Simpel
newCalcAA = zeros(2,2,5);
newCalcAA(1,1,:) = [11.47 31.56 130.07 39.87 0];
newCalcAA(1,2,:) = [0 0 0 0 221.99];
% Complex
newCalcAA(2,1,:) = [18.38 44.03 197.25 85.77 0];
newCalcAA(2,2,:) = [0 0 0 0 356.82];


plotBarStackGroups(oldCalc, {'Simpel', 'Complex'});
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Contouren zoeken', 'Pose van blokken zoeken', 'Blokken toevoegen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

plotBarStackGroups(newCalc, {'Simpel', 'Complex'});
axis([0.5 2.5 0 800])
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Threshold berekenen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

plotBarStackGroups(newCalcAA, {'Simpel', 'Complex'});
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Threshold berekenen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];