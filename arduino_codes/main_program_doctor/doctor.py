import os
import tkinter as tk
from tkinter import font as tkfont
from tkinter import messagebox, ttk
from PIL import Image, ImageTk
import pygame
import threading
import openai
import speech_recognition as sr
from gtts import gTTS
import datetime
import json
import subprocess
import aiohttp
import asyncio
import concurrent.futures
import serial  # pyserial 라이브러리

# pip install openai == 0.28.0
# pip install pygame
# pip install pillow
# pip install gTTS
# pip install SpeechRecognition
# pip install aiohttp

# 절대 경로 설정
base_dir = os.path.dirname(os.path.abspath(__file__))
image_path = os.path.join(base_dir, 'image', 'background.png')

class PatientPage(tk.Frame):
    def __init__(self, master):
        tk.Frame.__init__(self, master)
        self.create_widgets()
        self.load_patient_info()

        # 시리얼 포트 설정 (블루투스 모듈의 포트에 맞게 수정하세요)
        self.serial_port = serial.Serial('COM4', 9600)  # COM 포트를 블루투스 포트로 변경
        self.serial_port.timeout = 1

        # 데이터 업데이트를 위한 타이머 시작
        self.update_data()

    def update_data(self):
        try:
            if self.serial_port.in_waiting > 0:
                data = self.serial_port.readline().decode('utf-8').strip()
                # 데이터 형식: "TEM: xx.x C, BPM: xxx"
                if "TEM:" in data and "BPM:" in data:
                    temperature = data.split(",")[0].split(":")[1].strip()
                    bpm = data.split(",")[1].split(":")[1].strip()

                    # 데이터 표시
                    self.temperature_label.config(text=f"실시간 체온: {temperature} C")
                    self.bpm_label.config(text=f"실시간 심박수: {bpm} BPM")
        except Exception as e:
            print(f"데이터 수신 오류: {e}")

        # 일정 시간마다 갱신 (1000ms = 1초)
        self.after(1000, self.update_data)

    def create_widgets(self):
        set_background(self, "./image/background.png")
        self.create_top_bar()

        self.content_frame = tk.Frame(self, bg="#FAFAD2")
        self.content_frame.pack(fill="both", pady=50)

        # 실시간 데이터 표시 레이블
        self.temperature_label = tk.Label(self.content_frame, text="실시간 체온: -", bg="#FAFAD2", font=self.master.nanum_font)
        self.temperature_label.pack(pady=10)

        self.bpm_label = tk.Label(self.content_frame, text="실시간 심박수: -", bg="#FAFAD2", font=self.master.nanum_font)
        self.bpm_label.pack(pady=10)


class DoctorApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("공대최강팀")
        self.geometry("1024x600")
        self.resizable(False, False)

        self.nanum_font = tkfont.Font(family="NanumBarunGothic", size=12)

        self.frames = {}
        self.create_frames()
        self.show_frame("MainPage")

    def create_frames(self):
        for F in (MainPage, TalkingPage, PatientPage):
            frame = F(self)
            self.frames[F.__name__] = frame
            frame.place(x=0, y=0, width=1024, height=600)

    def show_frame(self, page_name, image_path=None):
        frame = self.frames[page_name]

        frame.tkraise()
        if page_name == "TalkingPage":
            frame.start_voice_interaction()

    def open_talking(self):
        self.show_frame("TalkingPage")

    def open_patient(self):
        self.show_frame("PatientPage")

# 배경화면 설정
def set_background(root, path):
    img = Image.open(path)
    bg_image = ImageTk.PhotoImage(img)
    background_label = tk.Label(root, image=bg_image)
    background_label.place(x=0, y=0, relwidth=1, relheight=1)
    background_label.image = bg_image  # 참조 유지

# 메인 화면
class MainPage(tk.Frame):
    def __init__(self, master):
        tk.Frame.__init__(self, master)
        self.create_widgets()

    def create_widgets(self):
        global image_path  # 전역 변수 사용
        set_background(self, image_path)
        self.create_main_top_bar()

        buttons_info = [
            (os.path.join(base_dir, "image", "doctor.png"), "증상 말하기", self.master.open_talking),
            (os.path.join(base_dir, "image", "patient.png"), "내 정보 작성하기", self.master.open_patient),
        ]

        button_size = 200
        text_height = 30

        for i, (image_path, text, command) in enumerate(buttons_info):
            if i == 0:  # Doctor button
                x = (1024 / 2) - button_size - 50  # Position to the left
            else:  # Patient button
                x = (1024 / 2) + 50  # Position to the right

            y = (600 - button_size - text_height) / 2
            self.create_image_button_with_text(image_path, text, command, x, y, button_size, text_height)

    def create_main_top_bar(self):
        # 상단바 생성
        top_bar = tk.Frame(self, bg="#F2F2F2", height=50)
        top_bar.pack(side="top", fill="x")

        # 전원 끄기 버튼
        power_img = Image.open(os.path.join(base_dir, 'image', 'power.png'))
        power_photo = ImageTk.PhotoImage(power_img.resize((40, 40), Image.LANCZOS))
        power_button = tk.Button(top_bar, image=power_photo, bg="#F2F2F2", command=self.ask_to_exit)
        power_button.image = power_photo
        power_button.pack(side="right", padx=(20, 30), pady=10)

        # 시간 표시 레이블
        self.time_label = tk.Label(top_bar, bg="#F2F2F2", font=self.master.nanum_font)
        self.time_label.pack(side="left", padx=20, pady=10)
        update_time(self.time_label)

    def ask_to_exit(self):
        if messagebox.askyesno("프로그램 종료", "프로그램을 종료하시겠습니까?"):
            self.master.destroy()

    def create_image_button_with_text(self, image_path, text, command, x, y, button_size, text_height):
        shadow_offset = 5

        # 그림자 레이블 생성
        shadow = tk.Label(self, bg='black')
        shadow.place(x=x + shadow_offset, y=y + shadow_offset, width=button_size, height=button_size)

        # 이미지 버튼 생성
        img = Image.open(image_path)
        img = ImageTk.PhotoImage(img)
        button = tk.Button(self, image=img, borderwidth=2, relief="solid", command=command)
        button.place(x=x, y=y, width=button_size, height=button_size)
        button.image = img  # 참조 유지

        # 텍스트 레이블 생성
        label_y = y + button_size + 10  # 텍스트의 y좌표
        label = tk.Label(self, text=text, bg='white', font=self.master.nanum_font)
        label.place(x=x, y=label_y, width=button_size, height=text_height)

        return button


# 대화하기 화면
class TalkingPage(tk.Frame):
    def __init__(self, master):
        tk.Frame.__init__(self, master)
        self.voice_assistant = VoiceAssistant(self)
        self.voice_thread = None
        self.voice_active = False
        self.create_widgets()
        self.current_y = 0

    def create_widgets(self):
        set_background(self, "./image/background.png")
        self.create_top_bar()

        self.chat_frame = tk.Frame(self, bg="#ADD8E6")  # 하늘색 프레임
        self.chat_frame.place(relx=0.08, rely=0.175, relwidth=0.6, relheight=0.75)
        self.chat_canvas = tk.Canvas(self.chat_frame, bg="#ADD8E6")
        self.chat_scrollbar = tk.Scrollbar(self.chat_frame, orient="vertical", command=self.chat_canvas.yview)
        self.chat_canvas.configure(yscrollcommand=self.chat_scrollbar.set)
        self.chat_scrollbar.pack(side="right", fill="y")
        self.chat_canvas.pack(side="left", fill="both", expand=True)

        self.chat_content_frame = tk.Frame(self.chat_canvas, bg="#ADD8E6")
        self.chat_canvas.create_window((0, 0), window=self.chat_content_frame, anchor="nw")

        # 대화를 시작하기 위한 안내 메시지
        custom_font = tkfont.Font(family="NanumBarunGothic", size=15)

        self.start_chat_label = tk.Label(self, text="대화를 시작하려면 '안녕하세요'라고 말해보세요.", font=custom_font)
        self.start_chat_label.pack()

        doctor_img = Image.open("./image/ai_doctor.png")
        doctor_photo = ImageTk.PhotoImage(doctor_img.resize((300, 300), Image.LANCZOS))

        image_label = tk.Label(self, image=doctor_photo, borderwidth=0, highlightthickness=0, bg="#FAFAD2")
        image_label.image = doctor_photo
        image_label.place(relx=0.84, rely=0.5, anchor="center")

    def update_scroll_region(self):
        self.chat_canvas.configure(scrollregion=self.chat_canvas.bbox("all"))

    def start_voice_interaction(self):
        if not self.voice_thread or not self.voice_thread.is_alive():
            self.voice_active = True
            self.voice_thread = threading.Thread(target=self.speak_voice, daemon=True)
            self.voice_thread.start()

    def speak_voice(self):
        recognizer = sr.Recognizer()
        # 음성 인식의 시작
        with sr.Microphone() as source:
            recognizer.adjust_for_ambient_noise(source, duration=1)
            with concurrent.futures.ThreadPoolExecutor() as executor:
                while self.voice_active:
                    try:
                        audio = recognizer.listen(source)
                        transcription = recognizer.recognize_google(audio, language='ko-KR').lower()
                        if transcription:
                            self.voice_assistant.process_user_input(transcription)
                            if "안녕하세요" in transcription:
                                self.start_chat_label.pack_forget()  # 안내 메시지 숨기기
                            if "종료" in transcription:
                                self.voice_active = False
                                break
                    except Exception as e:
                        continue  # 계속 듣기

    def reset_interaction(self):
        # 대화 상태 초기화
        self.voice_thread = None
        self.start_chat_label.pack()

    def stop_voice_interaction(self):
        self.voice_active = False

    # TTS 모델을 이용해서 gpt의 답변을 읽어줌
    def start_speak_text(self, text):
        tts_thread = threading.Thread(target=self.speak_text, args=(text,))
        tts_thread.start()

    def speak_text(self, text):
        tts = gTTS(text=text, lang='ko')
        filename = "./output.mp3"
        tts.save(filename)

        pygame.mixer.init()
        pygame.mixer.music.load(filename)
        pygame.mixer.music.play()

        while pygame.mixer.music.get_busy():
            continue

        pygame.mixer.music.unload()
        pygame.mixer.quit()

        os.remove(filename)

    def display_user_message(self, text):
        user_frame = tk.Frame(self.chat_canvas, bg="#FFFF00")
        user_label = tk.Label(user_frame, text=text, bg="#FFFF00", font=self.master.nanum_font, wraplength=250)
        user_label.pack(side="right", fill="both", expand=True, padx=(0, 20))

        # 프레임의 크기를 업데이트하고 위치 계산
        user_frame.update_idletasks()
        frame_height = user_frame.winfo_reqheight()
        self.chat_canvas.create_window((self.chat_frame.winfo_width(), self.current_y), window=user_frame, anchor="ne")

        # 다음 위젯의 y 좌표를 업데이트
        self.current_y += frame_height

        self.update_scroll_region()
        self.chat_canvas.yview_moveto(1.0)  # 새 메시지 추가 후 스크롤 맨 아래로

    def display_bot_message(self, text):
        bot_frame = tk.Frame(self.chat_canvas, bg="#FFFFFF")
        bot_label = tk.Label(bot_frame, text=text, bg="#FFFFFF", font=self.master.nanum_font, wraplength=250)
        bot_label.pack(side="left", fill="both", expand=True, padx=(20, 0))

        # 프레임의 크기를 업데이트하고 위치 계산
        bot_frame.update_idletasks()
        frame_height = bot_frame.winfo_reqheight()
        self.chat_canvas.create_window((0, self.current_y), window=bot_frame, anchor="nw")

        # 다음 위젯의 y 좌표를 업데이트
        self.current_y += frame_height

        self.update_scroll_region()
        self.chat_canvas.yview_moveto(1.0)  # 새 메시지 추가 후 스크롤 맨 아래로

    def reset_chat(self):
        for widget in self.chat_content_frame.winfo_children():
            widget.destroy()
        self.current_y = 0

    def create_top_bar(self):
        # 상단바 생성
        top_bar = tk.Frame(self, bg="#F2F2F2", height=50)
        top_bar.pack(side="top", fill="x")

        # 돌아가기 버튼
        back_img = Image.open("./image/left_arrow.png")
        back_photo = ImageTk.PhotoImage(back_img.resize((40, 40), Image.LANCZOS))
        back_button = tk.Button(top_bar, image=back_photo, bg="#F2F2F2", command=lambda: [self.stop_voice_interaction(), self.reset_interaction_and_return()])
        back_button.image = back_photo
        back_button.pack(side="left", padx=(20, 10), pady=10)

        # 돌아가기 텍스트
        back_label = tk.Label(top_bar, text="돌아가기", bg="#F2F2F2", font=self.master.nanum_font)
        back_label.pack(side="left", pady=10)

        # 시간 표시 레이블
        self.time_label = tk.Label(top_bar, bg="#F2F2F2", font=self.master.nanum_font)
        self.time_label.pack(side="right", padx=20, pady=10)
        update_time(self.time_label)

    # 돌아가기 버튼을 누르면 MainPage로 돌아가고 대화내역 초기화
    def reset_interaction_and_return(self):
        self.reset_chat()
        self.reset_interaction()
        self.master.show_frame("MainPage")


class VoiceAssistant:
    def __init__(self, talking_page):
        self.talking_page = talking_page
        openai.api_key = "sk-svcacct-yba5oF7yonJlaSuoavhXT3BlbkFJRj6f81YAyep52G3uVhW1"
        self.conversations = []
        self.messages = [
            {"role": "system", "content": "You work as a chatbot doctor at a general hospital, treating patients. Based on the patient's symptoms, provide clear and positive responses, and respond concisely to ensure the conversation is easy to understand. When asking the patient questions, ask only one question at a time. Do not inform the patient of any suspected diseases or diagnoses from the first question; instead, provide this information after obtaining sufficient answers about the patient's symptoms during the consultation. Help decide and implement the best treatment methods based on the diagnosis results. Respond with one sentence whenever possible. If the patient asks about a specific medication, provide information about that medication."}
        ]

    def process_user_input(self, text):
        self.conversations.append(f"[환자] {text}")  # 대화 내용 추가
        self.talking_page.display_user_message(text)

        # 환자의 음성에 '종료'가 포함되어 있을 경우 진료 종료
        if "종료" in text:
            farewell_message = "좋은 하루 되세요!"
            self.conversations.append(f"[AI의사] {farewell_message}")
            self.talking_page.display_bot_message(farewell_message)
            self.talking_page.start_speak_text(farewell_message)
            self.save_conversations()
            return

        asyncio.run(self.handle_response_async(text))

    async def handle_response_async(self, text):
        response = await self.generate_response_async(text)
        self.conversations.append(f"[AI의사] {response}")
        self.talking_page.display_bot_message(response)
        self.talking_page.start_speak_text(response)

    async def generate_response_async(self, text):
        self.messages.append({"role": "user", "content": text})

        async with aiohttp.ClientSession() as session:
            async with session.post(
                "https://api.openai.com/v1/chat/completions",
                headers={
                    "Authorization": f"Bearer {openai.api_key}",
                    "Content-Type": "application/json"
                },
                json={
                    "model": "gpt-4-turbo",
                    "messages": self.messages,
                    "max_tokens": 150,
                    "temperature": 0.3
                }
            ) as response:
                response_json = await response.json()
                response_text = response_json["choices"][0]["message"]["content"]
                self.messages.append({"role": "assistant", "content": response_text})
                return response_text

    # 진료 내역 저장
    def save_conversations(self):
        directory = "./diagnosis/"
        filename = "진료"
        file_extension = ".txt"
        file_count = 1

        while os.path.exists(f"{directory}{filename}{file_count}{file_extension}"):
            file_count += 1

        with open(f"{directory}{filename}{file_count}{file_extension}", "w", encoding="utf-8") as file:
            for line in self.conversations:
                file.write(line + "\n")

        self.conversations.clear()

    def transcribe_audio_to_text(self, audio_data, recognizer):
        try:
            return recognizer.recognize_google(audio_data, language='ko-KR')
        except Exception as e:
            pass

    def speak_text(self, text):
        tts = gTTS(text=text, lang='ko')
        filename = "./output.mp3"
        tts.save(filename)

        pygame.mixer.init()
        pygame.mixer.music.load(filename)
        pygame.mixer.music.play()

        while pygame.mixer.music.get_busy():
            continue

        pygame.mixer.music.unload()
        pygame.mixer.quit()

        os.remove(filename)


class PatientPage(tk.Frame):
    def __init__(self, master):
        tk.Frame.__init__(self, master)
        self.create_widgets()
        self.load_patient_info()

    def create_widgets(self):
        set_background(self, "./image/background.png")
        self.create_top_bar()

        self.content_frame = tk.Frame(self, bg="#FAFAD2")
        self.content_frame.pack(fill="both", pady=50)
        self.content_frame.grid_columnconfigure((3, 5), weight=1)

        self.labels = ["성", "이름", "생년월일", "전화번호", "주소지", "성별", "신장", "몸무게", "혈액형"]
        self.gender_var = tk.StringVar(value="남자")
        self.rh_negative_var = tk.BooleanVar(value=False)

        self.entry_fields = {}
        entry_width = 15

        for i, label in enumerate(self.labels):
            row = i
            tk.Label(self.content_frame, text=label, bg="#FAFAD2", font=self.master.nanum_font).grid(row=row, column=0, padx=(10, 2), pady=10, sticky="e")

            if label == "성별":
                tk.Radiobutton(self.content_frame, text="남자", bg="#FAFAD2", variable=self.gender_var, value="남자", font=self.master.nanum_font).grid(row=row, column=1, padx=(2, 20), sticky="w")
                tk.Radiobutton(self.content_frame, text="여자", bg="#FAFAD2", variable=self.gender_var, value="여자", font=self.master.nanum_font).grid(row=row, column=1, padx=(100, 0), sticky="w")
            elif label in ["신장", "몸무게", "혈액형", "성", "이름", "생년월일", "전화번호", "주소지"]:
                entry = tk.Entry(self.content_frame, width=entry_width)
                self.entry_fields[label] = entry
                entry.grid(row=row, column=1, sticky="ew", padx=5)
                if label in ["신장", "몸무게"]:
                    unit = "cm" if label == "신장" else "kg"
                    tk.Label(self.content_frame, text=unit, bg="#FAFAD2", font=self.master.nanum_font).grid(row=row, column=2, padx=5, sticky="w")
                if label == "혈액형":
                    tk.Checkbutton(self.content_frame, text="Rh -", bg="#FAFAD2", variable=self.rh_negative_var, font=self.master.nanum_font).grid(row=row, column=2, padx=(30, 0), sticky="w")

        save_button = tk.Button(self.content_frame, text="저장", font=self.master.nanum_font, command=self.save_patient_info)
        save_button.grid(row=len(self.labels) + 1, column=0, columnspan=3, pady=(1, 0), sticky="e")

        self.content_frame.grid_rowconfigure(len(self.labels), weight=1)

    def create_top_bar(self):
        top_bar = tk.Frame(self, bg="#F2F2F2", height=50)
        top_bar.pack(side="top", fill="x")

        back_img = Image.open("./image/left_arrow.png")
        back_photo = ImageTk.PhotoImage(back_img.resize((40, 40), Image.LANCZOS))
        back_button = tk.Button(top_bar, image=back_photo, bg="#F2F2F2", command=lambda: self.master.show_frame("MainPage"))
        back_button.image = back_photo
        back_button.pack(side="left", padx=(20, 10), pady=10)

        back_label = tk.Label(top_bar, text="돌아가기", bg="#F2F2F2", font=self.master.nanum_font)
        back_label.pack(side="left", pady=10)

        self.time_label = tk.Label(top_bar, bg="#F2F2F2", font=self.master.nanum_font)
        self.time_label.pack(side="right", padx=20, pady=10)
        update_time(self.time_label)

    def save_patient_info(self):
        patient_info = {
            "FamilyName": self.entry_fields["성"].get(),
            "GivenName": self.entry_fields["이름"].get(),
            "Birth": self.entry_fields["생년월일"].get(),
            "PhoneNumber": self.entry_fields["전화번호"].get(),
            "Address": self.entry_fields["주소지"].get(),
            "Gender": self.gender_var.get(),
            "Height": self.entry_fields["신장"].get(),
            "Weight": self.entry_fields["몸무게"].get(),
            "BloodType": self.entry_fields["혈액형"].get(),
            "RhNegative": self.rh_negative_var.get()
        }

        with open("patient_info.json", "w", encoding='utf-8') as file:
            json.dump(patient_info, file, ensure_ascii=False, indent=4)

    def load_patient_info(self):
        if os.path.exists("patient_info.json"):
            with open("patient_info.json", "r", encoding='utf-8') as file:
                patient_info = json.load(file)
                for key, value in patient_info.items():
                    if key == "FamilyName":
                        self.entry_fields["성"].insert(0, value)
                    elif key == "GivenName":
                        self.entry_fields["이름"].insert(0, value)
                    elif key == "Birth":
                        self.entry_fields["생년월일"].insert(0, value)
                    elif key == "PhoneNumber":
                        self.entry_fields["전화번호"].insert(0, value)
                    elif key == "Address":
                        self.entry_fields["주소지"].insert(0, value)
                    elif key == "Gender":
                        self.gender_var.set(value)
                    elif key == "Height":
                        self.entry_fields["신장"].insert(0, value)
                    elif key == "Weight":
                        self.entry_fields["몸무게"].insert(0, value)
                    elif key == "BloodType":
                        self.entry_fields["혈액형"].insert(0, value)
                    elif key == "RhNegative":
                        self.rh_negative_var.set(value)


# 시간 표시
def update_time(label):
    now = datetime.datetime.now()
    date_format = "%Y년 %m월 %d일"
    hour_format = "%I"  # 12시간 형식
    am_pm = now.strftime("%p").lower()  # 오전/오후 정보
    am_pm_kr = "오전" if am_pm == "am" else "오후"

    formatted_time = now.strftime(f"{date_format}   |   {am_pm_kr} {hour_format}시 %M분 %S초").replace(" 0", " ")
    label.config(text=formatted_time)
    label.after(1000, update_time, label)


# root.attributes("-fullscreen", True)  # 전체 화면 모드

# 앱 실행
if __name__ == "__main__":
    app = DoctorApp()
    app.mainloop()