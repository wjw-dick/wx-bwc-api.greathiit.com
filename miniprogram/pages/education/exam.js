const { request } = require('../../utils/request')
const util = require('../../utils/util')

Page({
  data: {
    id: null,
    exam: null,
    questions: [],
    answers: {},
    loading: true,
    submitting: false,
    result: null,
    error: ''
  },

  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },

  async load() {
    try {
      const exam = await request({ url: `/education/exams/${this.data.id}` })
      let questions = []
      try {
        questions = JSON.parse(exam.questionsJson || '[]')
      } catch (e) {
        questions = []
      }
      this.setData({
        exam: Object.assign({}, exam, {
          timeText: exam.startTime ? `${util.formatDate(exam.startTime)} ~ ${util.formatDate(exam.endTime)}` : '长期有效'
        }),
        questions: questions.map((q) => ({
          id: q.id,
          q: q.q,
          options: q.options || [],
          score: q.score || 0,
          selected: ''
        }))
      })
    } catch (e) {
      this.setData({ error: e.message || '考试加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  choose(e) {
    const index = Number(e.currentTarget.dataset.index)
    const value = e.detail.value
    this.setData({ [`questions[${index}].selected`]: value, error: '' })
  },

  async submit() {
    const unanswered = this.data.questions.filter((q) => !q.selected)
    if (unanswered.length) {
      return this.setData({ error: `还有 ${unanswered.length} 道题未作答` })
    }
    const answers = {}
    this.data.questions.forEach((q) => { answers[q.id] = q.selected })
    this.setData({ submitting: true, error: '' })
    try {
      const record = await request({
        url: `/education/exams/${this.data.id}/submit`,
        method: 'POST',
        data: { answers }
      })
      this.setData({ result: record })
      wx.showToast({ title: record.passFlag === 1 ? '考试通过' : '考试未通过', icon: 'none' })
    } catch (e) {
      this.setData({ error: e.message || '提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goBack() {
    wx.navigateBack()
  }
})
