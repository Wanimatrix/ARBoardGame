oldCalc = zeros(2,2,7);
% Simpel
oldCalc(1,1,:) = [18.61 54.37 58.58 18.54 1.35 12.15 0];
oldCalc(1,2,:) = [0 0 0 0 0 0 173.03];
% Complex
oldCalc(2,1,:) = [18.76 58.05 68.33 50.12 3.02 36.08 0];
oldCalc(2,2,:) = [0 0 0 0 0 0 245.15];

oldLemmingUpdate = [72.906493;0.671386;0.701904;1.037597;1.434326;0.305175;0.335693;53.07007;0.305176;0.488281;0.549316;3.082275;87.493896;0.854492;0.549317;0.854492;0.488281;96.954345;1.464844;0.823975;114.532469;5.157471;0.335693;0.518799;52.032471;0.427246;0.427246;1.312256;0.793457;131.317141;0.823975;0.457763;3.082277;70.404052;0.579834;0.427246;0.823974;132.263184;0.427246;0.579834;0.793457;52.642822;0.457764;0.610352;0.488281;0.427247;45.440675;0.335693;0.366211;0.457764;2.258301;53.588866;0.976562;0.823975;0.488281;57.678223;0.457764;0.610352;0.885009;94.055176;0.549317;0.76294;0.427246;0.457764;111.053468;0.549317;0.579834;0.549316;104.217529;2.075195;0.88501;72.174072;4.821777;0.427246;0.488281;0.396729;53.71094;0.823975;0.549316;0.488281;41.687012;0.640869;0.701904;0.305176;15.533448;0.335693;0.366211;0.396729;0.305176;0.335693;0.305176;41.22925;0.36621;0.640869;0.549316;8.605957;0.427246;45.837402;0.457764;0.427246;0.793457;0.488281;32.653807;0.518799;0.762939;0.88501;56.182863;0.76294;0.518799;0.640869;84.747315;0.579834;0.915527;0.427246;52.307128;0.488281;0.579834;0.549316;27.008057;1.00708;0.701904;0.488281;1.098632;13.885499;0.457764;0.671386;0.762939;42.327882;0.701904;0.579834;0.579834;0.518799;31.402586;0.274659;0.579834;0.610352;0.488281;26.611327;9.216308;2.532959;0.610351;13.488768;1.525879;0.640869;0.915528;20.294189;0.671387;0.823975;0.640869;0.579834;9.429931;0.518798;1.40381;1.403809;0.518799;41.412355;0.640869;0.518798;0.518798;8.911132;0.640869;0.579834;1.251221;0.305176;6.56128;0.732422;1.373291;0.854492;0.549317;6.561279;0.488281;0.915529;0.976562;0.549317;5.065917;0.610351;0.946045;0.64087;32.62329;1.098634;9.674072;0.488281;14.282227;0.305175;0.823975;0.88501;0.305176;5.981446;0.335693;0.946045;0.488281;0.457764;6.408692;0.427246;0.579834;0.488281;0.793457;5.493165;0.610352;0.610352;0.671387;12.298584;0.915527;1.678467;0.671387;13.061524;0.640869;0.640869;0.976563;5.004882;0.579834;0.579834;1.068115;18.280029;0.640869;0.946045;0.488281;0.762939;2.807618;0.427246;0.640869;0.549317;0.946045;2.685546;0.76294;0.549316;11.413573;1.983643;0.488282;0.427246;0.579834;1.251221;0.488282;0.610351;1.00708;1.953125;0.701904;0.671386;0.610352;0.823975;0.88501;155.212404;0.305175;0.762939;0.366211;5.523682;0.640869;160.064695;0.793457;0.427246;0.518799;0.640869;108.09326;0.518799;0.488281;0.885009;0.488281;99.79248;0.549317;0.579834;0.457764;0.427246;69.335938;0.457764;0.427246;0.854492;83.343505;0.518799;1.312256;0.488282;78.033447;0.579834;0.579834;0.518799;0.579834;75.256347;2.288818;0.732422;0.579834;77.178955;0.335693;0.335694;0.427246;0.335693;87.677002;0.701904;0.427246;0.854492;0.427246;66.345212;0.549316;0.549316;0.885009;53.375243;0.518799;0.732422;0.457764;0.732422;45.379637;0.488281;3.692626;1.464843;123.596191;0.396729;0.579835;0.305175;0.366211;113.433839;1.037597;0.427246;0.946045;0.427247;133.056641;1.159668;0.457763;0.457763;17.211915;0.274658;0.335693;0.488282;0.366211;0.335693;0.823975;32.623293;0.671387;0.549316;0.915527;0.488281;54.077146;0.701904;1.464845;0.976563;98.052978;0.823975;0.549316;54.107665;4.211426;0.854492;0.610351;41.625978;0.640869;0.518799;0.549316;0.762939;47.973635;0.915528;0.88501;0.732422;0.793458;65.093995;0.762939;2.105713;1.251222;57.434083;2.929687;0.762939;71.31958;0.732422;0.610352;0.823975;50.140382;0.915528;0.518799;36.071775;1.159668;3.387451;0.579834;28.289797;1.89209;0.518799;0.762939;38.726808;0.518798;0.885011;0.885009;44.342041;0.823975;0.946046;0.915528;37.719726;6.134034;0.823974;9.06372;19.073486;0.610351;0.549317;0.701905;11.59668;0.518798;0.427246;0.854492;0.488281;7.110595;0.579834;0.549316;0.762939;6.195068;0.885009;0.823975;0.671386;7.293701;0.885009;0.488281;1.220703;11.383057;0.885011;0.671387;0.579834;0.671386;7.873535;0.854492;0.854492;0.76294;7.080078;0.793457;0.579834;6.103516;0.701904;0.610351;1.495361;5.310059;0.610351;1.220703;1.434326;6.347656;2.502441;0.305176;0.366211;0.36621;0.335694;2.258301;0.396729;0.427246;0.823976;0.457764;0.36621;3.02124;0.335693;0.640869;0.518799;0.427246;0.427247;2.685547;0.701904;0.732421;0.488281;0.610352;0.335694;3.295899;0.335693;0.488282;0.885009;0.366211;0.305175;1.586914;0.518799;0.335693;0.366211;0.366211;0.518799;2.2583;0.640869;0.701904;0.88501;0.701904;2.899169;0.885009;0.640869;0.732422;4.302978;0.793457;0.640869;0.549317;4.91333;1.831055;0.946046;0.88501;2.502442;0.946045;0.823975;0.823974;3.540039;0.488281;1.00708;0.915527;0.732422;185.974121;0.701904;0.854492;1.800537;0.793457;148.376465;0.854492;0.335694;10.83374;148.55957;0.793457;10.31494;88.897706;0.762939;0.823975;1.40381;123.565675;0.457764;0.488281;0.88501;165.374755;0.854492;0.946046;0.793457;114.562988;1.098633;0.793457;1.434326;74.34082;0.854492;0.88501;5.187989;88.623047;0.610352;0.76294;0.671387;58.532716;0.549317;0.671387;0.549316;62.927247;0.762939;0.549316;0.701905;68.206787;3.845216;0.671386;0.762939;107.177734;0.488281;0.793457;1.373291;91.186522;0.671386;0.640869;45.410153;8.697511;0.610351;0.335693;0.885009;0.396729;0.366211;85.510253;0.457764;0.762939;0.976562;45.227052;0.610352;0.549317;0.732422;69.213866;0.427246;0.335693;0.701904;0.732422;83.465575;0.396729;1.159668;0.457763;32.562256;2.13623;0.610351;0.488282;0.732422;79.986572;0.701905;0.701904;0.854492;95.184325;0.976562;0.701904;1.403808;53.283692;1.00708;0.427246;0.671387;4.699707;21.270751;0.488281;1.00708;0.457764;45.166015;0.701904;4.730225;0.671386;1.037597;32.684326;0.457764;2.441406;0.610352;29.876709;0.854492;0.640869;0.640869;0.305176;14.007567;0.305175;0.549316;0.366211;5.096435;37.963869;0.793457;0.701904;0.762939;28.74756;1.068115;1.037599;0.579834;34.973145;0.579834;0.823975;0.518799;6.530761;15.624999;0.762939;0.671387;2.89917;9.277343;0.88501;0.671387;0.793457;17.181395;0.549316;1.251221;0.854492;0.976562;8.666992;0.88501;0.946045;0.701904;6.317139;0.579834;0.762939;11.199952;0.610352;0.549317;1.25122;19.927979;0.518798;1.678467;0.823975;9.948731;0.946044;1.220703;0.488281;9.002686;0.854492;0.88501;7.659912;0.549317;0.701905;0.427246;0.854492;4.54712;1.159668;0.488281;0.640869;0.823975;0.457764;11.59668;0.915527;1.098632;0.457763;6.866456;0.427246;1.190186;0.701904;3.295898;6.134032;0.579834;0.427246;0.335694;0.579834;0.427247;3.082275;2.65503;0.732422;0.732422;4.852294;0.823974;0.823974;1.037597;1.434326;1.00708;0.76294;0.854492;2.044678;0.427246;0.701904;0.549316;3.204347;0.915528;1.281738;0.88501;2.166748;20.965575];
% Simpel
newCalc = zeros(2,2,5);
newCalc(1,1,:) = [26.07 52.15 240.83 191.86 0];
newCalc(1,2,:) = [0 0 0 0 513.86];
% Complex
newCalc(2,1,:) = [17.82 61.57 259.12 578.09 0];
newCalc(2,2,:) = [0 0 0 0 913.84];

% Simpel
newCalcAA = zeros(2,2,5);
newCalcAA(1,1,:) = [19.08 71.76 295.81 27.89 0];
newCalcAA(1,2,:) = [0 0 0 0 417.75];
% Complex
newCalcAA(2,1,:) = [18.84 66.44 299.15 33.75 0];
newCalcAA(2,2,:) = [0 0 0 0 427.78];


plotBarStackGroups(oldCalc, {'Simpel', 'Complex'});
axis([0.5 2.5 0 (oldCalc(2,2,7)+200)])
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Contouren zoeken', 'Pose van blokken zoeken', 'Blokken toevoegen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

plotBarStackGroups(newCalc, {'Simpel', 'Complex'});
axis([0.5 2.5 0 (newCalc(2,2,5)+200)])
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Threshold berekenen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

plotBarStackGroups(newCalcAA, {'Simpel', 'Complex'});
axis([0.5 2.5 0 (newCalcAA(2,2,5)+300)])
ylabel('Time (ms)');
% Add a legend
h = legend('YUV naar RGB', 'Berekening camerapose', 'Threshold berekenen', 'Lemming update', 'Totaal');
h.Position = [0.15 0.6 0.3 0.3];

figure
plot(1:1:size(oldLemmingUpdate,1),oldLemmingUpdate);