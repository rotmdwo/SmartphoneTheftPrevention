<h2> 소스코드 파일에 대한 설명 </h2>

<h3>안드로이드 어플리케이션</h3>
<ul>
<li> app/src/main/java/edu/skku/cs/autosen/sensor : 데이터 수집, 사용자 인증을 관장하는 파일들의 디렉토리 </li>
<li> app/src/main/java/edu/skku/cs/autosen/api : 서버와 통신하는 API들을 담고 있는 디렉토리 </li>
<li> app/src/main/java/edu/skku/cs/autosen/utility/Utility.kt : 어플리케이션 전역에 사용된 메서드들을 재사용하기 위해 담아둔 파일 </li>
</ul>

<h3>서버</h3>
<ul>
<li> Server/src/main/kotlin/edu/cs/skku/autosen_server/controller/Controller.kt : 서버 API를 담는 파일 </li>
<li> Server/src/main/kotlin/edu/cs/skku/autosen_server/train/TrainProcess.kt : 모델학습 API 요청 처리를 위한 파일 </li>
<li> Server/src/main/kotlin/edu/cs/skku/autosen_server/utility/Utility.kt : 서버 전역에 사용된 메서드들을 재사용하기 위해 담아둔 파일 </li>
<li> data : 사용자들의 센서 데이터들과 메타정보가 저장되는 디렉토리 </li>
</ul>

<h3>딥러닝 모델</h3>
<ul>
<li> model/TrainModel.py : 서버가 모델학습 요청을 받았을 때 실행시키는 파일 </li>
<li> model/LoadModel.py : 서버가 사용자 인증 요청을 받았을 때 실행시키는 파일 </li>
<li> model/models : 사용자들의 딥러닝 모델이 저장되는 디렉토리 </li>
</ul>

<h3>시스템을 실행하기 위해서는</h3>
<ol> 
<li> app/src/main/java/edu/skku/cs/autosen/api/ApiGenerator.kt 파일의 HOST 변수를 서버를 구축하는 컴퓨터의 외부 IP로 설정해주세요. </li>
<li> Server/src/main/kotlin/edu/cs/skku/autosen_server/ServerApplication.kt 파일을 실행시켜주세요. </li>
<li> 공유기를 사용한다면 포트 포워딩해주세요. </li>
<li> app/autosen_apk.apk 파일을 안드로이드 스마트폰에 설치하여 실행해주세요. </li>
</ol>
